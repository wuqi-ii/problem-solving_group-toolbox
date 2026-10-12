package cn.edu.teamtoolbox.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.Instant;

public interface TaskRepository extends JpaRepository<TaskEntity, String> {
    List<TaskEntity> findAllByGroupIdOrderByCreatedAtDesc(String groupId);
    List<TaskEntity> findAllByStatusInAndDueAtBetween(List<TaskStatus> statuses, Instant start, Instant end);
    List<TaskEntity> findAllByStatusInAndDueAtBefore(List<TaskStatus> statuses, Instant dueAt);
}
