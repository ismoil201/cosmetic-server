package com.example.backend.controller;

import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    // ✔️ 1. Barcha userlar
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ✔️ 2. User by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✔️ 3. User yaratish (Adminlar yaratishi mumkin)
    @PostMapping
    public User createUser(@RequestBody User user) {
        // ROLE bo‘lmasa default USER
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        return userRepository.save(user);
    }

    // ✔️ 4. User update
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody User updated
    ) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setFullName(updated.getFullName());
                    user.setEmail(updated.getEmail());
                    user.setPassword(updated.getPassword());
                    user.setRole(updated.getRole());
                    userRepository.save(user);
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ✔️ 5. User o‘chirish
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    userRepository.delete(user);
                    return ResponseEntity.ok("User deleted");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ✔️ 6. Role o'zgartirish
    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestParam String role) {

        return userRepository.findById(id)
                .map(user -> {
                    user.setRole(Role.valueOf(role.toUpperCase()));
                    userRepository.save(user);
                    return ResponseEntity.ok("Role updated!");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
