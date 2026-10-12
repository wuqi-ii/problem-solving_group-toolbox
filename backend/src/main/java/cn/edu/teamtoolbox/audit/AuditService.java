package cn.edu.teamtoolbox.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AuditService {
    private final AuditLogRepository repository;
    public AuditService(AuditLogRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String userId, String method, String path, int status, String clientIp) {
        repository.save(new AuditLogEntity(userId, method, path, status, clientIp));
    }

    @Transactional(readOnly = true)
    public List<AuditLogView> listMine(String userId) {
        return repository.findTop100ByUserIdOrderByOccurredAtDesc(userId).stream()
                .map(item -> new AuditLogView(item.getId(), item.getMethod(), item.getPath(),
                        item.getResponseStatus(), item.getClientIp(), item.getOccurredAt())).toList();
    }
}
