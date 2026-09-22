package cn.edu.teamtoolbox.notification;

import java.time.Instant;

public record NotificationView(String id, String type, String title, String content, String targetPath,
                               boolean read, Instant createdAt) {}
