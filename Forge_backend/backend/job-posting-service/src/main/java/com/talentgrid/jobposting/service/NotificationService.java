package com.talentgrid.jobposting.service;

import com.talentgrid.jobposting.dto.response.NotificationResponse;
import com.talentgrid.jobposting.entity.Notification;
import com.talentgrid.jobposting.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void send(Long userId, String userEmail, Long jobPostingId, String type, String title, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .userEmail(userEmail)
                .jobPostingId(jobPostingId)
                .type(type)
                .title(title)
                .message(message)
                .build();
        notificationRepository.save(notification);
        log.info("Notification sent to userId={} type={} jobPostingId={}", userId, type, jobPostingId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadForUser(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countUnread(Long userId) {
        return Map.of("unread", notificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadForUser(userId);
    }

    @Transactional
    public void markOneRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
    }
}
