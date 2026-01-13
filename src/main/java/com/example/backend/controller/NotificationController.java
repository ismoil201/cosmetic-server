package com.example.backend.controller;

import com.example.backend.dto.FcmTokenRequest;
import com.example.backend.dto.NotificationResponse;
import com.example.backend.entity.Notification;
import com.example.backend.entity.User;
import com.example.backend.entity.UserFcmToken;
import com.example.backend.repository.NotificationRepository;
import com.example.backend.repository.UserFcmTokenRepository;
import com.example.backend.service.UserService;
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

