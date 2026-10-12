package cn.edu.teamtoolbox.collaboration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollaborationPostRepository extends JpaRepository<CollaborationPostEntity, String> {
    List<CollaborationPostEntity> findAllByGroupIdAndTypeAndStatusOrderByPinnedDescCreatedAtDesc(
            String groupId, CollaborationPostType type, String status);
    List<CollaborationPostEntity> findAllByTaskIdAndTypeAndStatusOrderByCreatedAtAsc(
            String taskId, CollaborationPostType type, String status);
}
