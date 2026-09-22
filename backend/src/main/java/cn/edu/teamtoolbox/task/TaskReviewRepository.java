package cn.edu.teamtoolbox.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskReviewRepository extends JpaRepository<TaskReviewEntity, String> {
    Optional<TaskReviewEntity> findBySubmissionId(String submissionId);
}
