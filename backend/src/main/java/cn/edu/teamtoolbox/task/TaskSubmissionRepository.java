package cn.edu.teamtoolbox.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskSubmissionRepository extends JpaRepository<TaskSubmissionEntity, String> {
    Optional<TaskSubmissionEntity> findTopByTaskIdOrderByVersionNoDesc(String taskId);

    List<TaskSubmissionEntity> findAllByTaskIdOrderByVersionNoDesc(String taskId);

    boolean existsByAttachmentFileId(String attachmentFileId);
}
