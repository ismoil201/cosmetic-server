package com.example.backend.service;

import com.example.backend.entity.UserFcmToken;
import com.example.backend.repository.UserFcmTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class FcmService {

    private final UserFcmTokenRepository tokenRepo;

    public void sendToUser(
            Long userId,
            String title,
            String body,
            Map<String, String> data
    ) {

        List<UserFcmToken> tokens = tokenRepo.findByUserId(userId);

        for (UserFcmToken t : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(t.getToken())

                        // 🔔 SYSTEM notification (ENG MUHIM)
                        .setNotification(
                                com.google.firebase.messaging.Notification.builder()
                                        .setTitle(title)   // 👈 "Sansajr"
                                        .setBody(body)     // 👈 "salom"
                                        .build()
                        )

                        // 📦 DATA (ixtiyoriy)
                        .putData("type", "ANNOUNCEMENT")
                        .putData("refId", "123")

                        .build();

                FirebaseMessaging.getInstance().send(message);


            } catch (Exception e) {
                tokenRepo.deleteByToken(t.getToken());
            }
        }
    }
}
