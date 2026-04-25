package com.example.backend.domain.user.service;

import com.example.backend.domain.user.entity.UserFcmToken;
import com.example.backend.domain.user.repository.UserFcmTokenRepository;
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

                        // 🔴 SHU JOY ENG MUHIM
                        .setNotification(
                                Notification.builder()
                                        .setTitle("Zaven")          // app nomi
                                        .setBody("Yangi aksiya 🎉") // matn
                                        .build()
                        )

                        // data optional (click handling uchun)
                        .putData("type", "ANNOUNCEMENT")
                        .build();

                FirebaseMessaging.getInstance().send(message);


            } catch (Exception e) {
                tokenRepo.deleteByToken(t.getToken());
            }
        }
    }
}
