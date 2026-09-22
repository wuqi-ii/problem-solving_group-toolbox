package cn.edu.teamtoolbox.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskSubmissionRepository extends JpaRepository<TaskSubmissionEntity, String> {
    Optional<TaskSubmissionEntity> findTopByTaskIdOrderByVersionNoDesc(String taskId);
}
