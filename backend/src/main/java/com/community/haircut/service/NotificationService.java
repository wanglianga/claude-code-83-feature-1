package com.community.haircut.service;

import com.community.haircut.entity.Notification;
import com.community.haircut.entity.User;
import com.community.haircut.enums.Role;
import com.community.haircut.repository.NotificationRepository;
import com.community.haircut.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void notify(Long userId, String title, String content, String type) {
        if (userId == null) {
            return;
        }
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        notificationRepository.save(n);
    }

    public void notifyRole(Role role, String title, String content, String type) {
        List<User> users = userRepository.findByRoleAndStatus(role, "ACTIVE");
        for (User u : users) {
            notify(u.getId(), title, content, type);
        }
    }
}
