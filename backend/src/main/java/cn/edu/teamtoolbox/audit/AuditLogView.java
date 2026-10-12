package cn.edu.teamtoolbox.audit;

import java.time.Instant;

public record AuditLogView(String id, String method, String path, int responseStatus, String clientIp, Instant occurredAt) {}
