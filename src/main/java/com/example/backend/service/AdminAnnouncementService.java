package com.example.backend.service;

import com.example.backend.dto.AdminAnnouncementResponse;
import com.example.backend.entity.*;
import com.example.backend.repository.AnnouncementRepository;
import com.example.backend.repository.NotificationRepository;
import com.example.backend.repository.UserFcmTokenRepository;
import com.example.backend.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAnnouncementService {

    private final AnnouncementRepository announcementRepo;
    private final UserRepository userRepo;
    private final NotificationRepository notificationRepo;
    private final UserFcmTokenRepository tokenRepo;



    public Page<AdminAnnouncementResponse> list(
            Boolean active,
            String type,
            Pageable pageable
    ) {

        NotificationType nt = null;
        if (type != null) {
            nt = NotificationType.valueOf(type.toUpperCase());
        }

        Page<Announcement> page;

        if (active != null && nt != null) {
            page = announcementRepo.findByActiveAndType(active, nt, pageable);
        } else if (active != null) {
            page = announcementRepo.findByActive(active, pageable);
        } else if (nt != null) {
            page = announcementRepo.findByType(nt, pageable);
        } else {
            page = announcementRepo.findAll(pageable);
        }

        return page.map(a -> new AdminAnnouncementResponse(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.getType(),
                a.isActive(),
                a.getCreatedAt()
        ));
    }

    // ================= SOFT DELETE =================
    @Transactional
    public void deactivate(Long id) {
        Announcement a = announcementRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));
        a.setActive(false);
        announcementRepo.save(a);
    }

    // ================= RESTORE =================
    @Transactional
    public void restore(Long id) {
        Announcement a = announcementRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));
        a.setActive(true);
        announcementRepo.save(a);
    }

    // ================= HARD DELETE (OPTIONAL) =================
    @Transactional
    public void delete(Long id) {
        announcementRepo.deleteById(id);
    }
    @Transactional
    public void publish(String title, String content, NotificationType type) {

        Announcement a = new Announcement();
        a.setTitle(title);
        a.setContent(content);
        a.setType(type);
        a.setActive(true);
        announcementRepo.save(a);

        // 🔔 DB notification
        for (User user : userRepo.findAll()) {
            Notification n = new Notification();
            n.setUser(user);
            n.setTitle(title);
            n.setMessage(content);
            n.setType(type);
            n.setRefId(a.getId());
            notificationRepo.save(n);
        }

        // 🔥 FCM push
        sendFcm(title, content);
    }

    private void sendFcm(String title, String body) {

        for (UserFcmToken t : tokenRepo.findAll()) {
            try {
                Message msg = Message.builder()
                        .setToken(t.getToken())
                        .setNotification(
                                com.google.firebase.messaging.Notification
                                        .builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build()
                        )
                        .putData("type", "ANNOUNCEMENT")
                        .build();

                FirebaseMessaging.getInstance().sendAsync(msg);
            } catch (Exception e) {
                tokenRepo.deleteByToken(t.getToken());
            }
        }
    }



}
