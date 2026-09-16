package com.community.haircut.controller;

import com.community.haircut.common.BizException;
import com.community.haircut.common.Result;
import com.community.haircut.entity.Notification;
import com.community.haircut.repository.NotificationRepository;
import com.community.haircut.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public Result<List<Notification>> list() {
        return Result.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(SecurityUtils.uid()));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationRepository.countByUserIdAndReadFlagFalse(SecurityUtils.uid()));
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        Notification n = notificationRepository.findById(id).orElseThrow(() -> new BizException("通知不存在"));
        if (!n.getUserId().equals(SecurityUtils.uid())) {
            throw new BizException(403, "无权限");
        }
        n.setReadFlag(true);
        notificationRepository.save(n);
        return Result.ok();
    }

    @PostMapping("/read-all")
    public Result<Void> readAll() {
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(SecurityUtils.uid());
        list.forEach(n -> n.setReadFlag(true));
        notificationRepository.saveAll(list);
        return Result.ok();
    }
}
