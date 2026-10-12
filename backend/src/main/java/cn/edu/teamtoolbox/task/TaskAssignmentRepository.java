package cn.edu.teamtoolbox.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignmentEntity, String> {
    List<TaskAssignmentEntity> findAllByTaskIdAndStatus(String taskId, String status);
    boolean existsByTaskIdAndUserIdAndStatus(String taskId, String userId, String status);
    List<TaskAssignmentEntity> findAllByTaskId(String taskId);
    List<TaskAssignmentEntity> findAllByUserIdAndStatus(String userId, String status);
    Optional<TaskAssignmentEntity> findByTaskIdAndUserId(String taskId, String userId);
}
