package cn.edu.teamtoolbox.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {
    @Id @Column(name = "audit_id", length = 36, nullable = false)
    private String id;
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;
    @Column(name = "http_method", length = 10, nullable = false)
    private String method;
    @Column(name = "request_path", length = 255, nullable = false)
    private String path;
    @Column(name = "response_status", nullable = false)
    private int responseStatus;
    @Column(name = "client_ip", length = 64)
    private String clientIp;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditLogEntity() {}
    public AuditLogEntity(String userId, String method, String path, int responseStatus, String clientIp) {
        this.id = UUID.randomUUID().toString(); this.userId = userId; this.method = method; this.path = path;
        this.responseStatus = responseStatus; this.clientIp = clientIp; this.occurredAt = Instant.now();
    }
    public String getId() { return id; } public String getUserId() { return userId; }
    public String getMethod() { return method; } public String getPath() { return path; }
    public int getResponseStatus() { return responseStatus; } public String getClientIp() { return clientIp; }
    public Instant getOccurredAt() { return occurredAt; }
}
