package com.example.backend.domain.auth.service;

import com.example.backend.domain.auth.dto.LoginRequest;
import com.example.backend.domain.auth.dto.LoginResponse;
import com.example.backend.domain.auth.dto.RegisterRequest;
import com.example.backend.domain.user.entity.AuthProvider;
import com.example.backend.domain.user.entity.Role;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.repository.UserRepository;
import com.example.backend.global.security.JwtService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // =========================
    // ✅ REGISTER (EMAIL/PASSWORD)
    // =========================
    public void register(RegisterRequest req) {

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName());
        user.setPhone(req.getPhone());
        user.setRole(Role.USER);
        user.setProvider(AuthProvider.LOCAL);

        userRepository.save(user);
    }

    // =========================
    // ✅ LOGIN (EMAIL/PASSWORD)
    // =========================
    public LoginResponse login(LoginRequest req) {

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Wrong password");
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                token
        );
    }

    // =========================
    // ✅ FIREBASE LOGIN (GOOGLE / PHONE)
    // =========================
    public LoginResponse firebaseLogin(String idToken) {
        try {
            // 1️⃣ Verify Firebase ID token
            FirebaseToken token =
                    FirebaseAuth.getInstance().verifyIdToken(idToken);

            String uid = token.getUid();
            String emailFromToken = token.getEmail();
            String name = token.getName();

            // 2️⃣ Get full Firebase user (PHONE shu yerdan olinadi)
            UserRecord userRecord =
                    FirebaseAuth.getInstance().getUser(uid);

            String phone = userRecord.getPhoneNumber(); // 📱 PHONE (bo‘lishi mumkin yoki null)

            // 3️⃣ PROVIDER ANIQLASH
            AuthProvider detectedProvider;
            if (phone != null) {
                detectedProvider = AuthProvider.PHONE;
            } else if (emailFromToken != null) {
                detectedProvider = AuthProvider.GOOGLE;
            } else {
                detectedProvider = AuthProvider.LOCAL;
            }

            final AuthProvider provider = detectedProvider;

            // 4️⃣ EMAIL FALLBACK (PHONE LOGINDA EMAIL YO‘Q BO‘LISHI MUMKIN)
            final String email = (emailFromToken != null)
                    ? emailFromToken
                    : uid + "@firebase.user";

            // 5️⃣ USER FIND OR CREATE
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User u = new User();
                        u.setEmail(email);
                        u.setFullName(name != null ? name : "User");
                        u.setPhone(phone);
                        u.setRole(Role.USER);
                        u.setProvider(provider);
                        u.setProviderId(uid);
                        return userRepository.save(u);
                    });

            // 6️⃣ GENERATE BACKEND JWT
            String jwt = jwtService.generateToken(
                    user.getEmail(),
                    user.getRole().name()
            );

            return new LoginResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    jwt
            );

        } catch (Exception e) {
            throw new RuntimeException("Invalid Firebase token", e);
        }
    }
}
