package cn.edu.teamtoolbox.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignmentEntity, String> {
    List<TaskAssignmentEntity> findAllByTaskIdAndStatus(String taskId, String status);
    boolean existsByTaskIdAndUserIdAndStatus(String taskId, String userId, String status);
}
