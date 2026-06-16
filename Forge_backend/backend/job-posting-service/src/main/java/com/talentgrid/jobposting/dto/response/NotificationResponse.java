package com.talentgrid.jobposting.dto.response;

import com.talentgrid.jobposting.entity.Notification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Mirrors the frontend AppNotification TypeScript interface exactly.
 */
@Data
@Builder
public class NotificationResponse {

    private Long id;
    private Long userId;
    private Long jobPostingId;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .jobPostingId(n.getJobPostingId())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
