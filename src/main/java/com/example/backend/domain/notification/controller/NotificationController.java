package com.example.backend.domain.notification.controller;

import com.example.backend.domain.notification.dto.FcmTokenRequest;
import com.example.backend.domain.notification.dto.NotificationResponse;
import com.example.backend.domain.notification.entity.Notification;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.entity.UserFcmToken;
import com.example.backend.domain.notification.repository.NotificationRepository;
import com.example.backend.domain.user.repository.UserFcmTokenRepository;
import com.example.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepo;
    private final UserFcmTokenRepository tokenRepo;
    private final UserService userService;

    @GetMapping
    public List<NotificationResponse> myNotifications() {

        User user = userService.getCurrentUser();

        return notificationRepo
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(n -> new NotificationResponse(
                        n.getId(),
                        n.getTitle(),
                        n.getMessage(),
                        n.getType().name(),
                        n.isRead(),
                        n.getCreatedAt(),
                        n.getRefId()
                ))
                .toList();
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {

        User user = userService.getCurrentUser();

        Notification n = notificationRepo.findById(id)
                .filter(notif -> notif.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        n.setRead(true);
        notificationRepo.save(n);
    }


    @PostMapping("/token")
    public void saveToken(@RequestBody FcmTokenRequest req) {

        User user = userService.getCurrentUser();

        // 🔥 duplicate token bo‘lmasin
        tokenRepo.deleteByToken(req.getToken());

        UserFcmToken token = new UserFcmToken();
        token.setUserId(user.getId());
        token.setToken(req.getToken());

        tokenRepo.save(token);
    }
    @GetMapping("/unread-count")
    public long unreadCount() {
        User user = userService.getCurrentUser();
        return notificationRepo.countByUserIdAndIsReadFalse(user.getId());
    }



}

