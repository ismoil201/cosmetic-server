package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;

    // 📦 ORDER STATUS
    public void orderStatusChanged(
            User user,
            Order order,
            OrderStatus status
    ) {

        String title;
        String message;

        switch (status) {
            case SHIPPED -> {
                title = "Buyurtma jo‘natildi 📦";
                message = "Buyurtmangiz yo‘lga chiqdi";
            }
            case DELIVERED -> {
                title = "Buyurtma yetib keldi 🎉";
                message = "Buyurtmangiz manzilga yetkazildi";
            }
            case CANCELED -> {
                title = "Buyurtma bekor qilindi ❌";
                message = "Buyurtmangiz bekor qilindi";
            }
            default -> {
                title = "Buyurtma holati o‘zgardi";
                message = "Yangi holat: " + status.name();
            }
        }

        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(NotificationType.ORDER);
        n.setRefId(order.getId());

        notificationRepo.save(n);
    }

    // 📢 ADMIN E’LON
    public void sendSystem(User user, String title, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(NotificationType.SYSTEM);

        notificationRepo.save(n);
    }
}
