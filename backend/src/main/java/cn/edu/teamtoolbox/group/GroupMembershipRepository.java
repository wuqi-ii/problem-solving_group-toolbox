package cn.edu.teamtoolbox.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMembershipRepository extends JpaRepository<GroupMembershipEntity, String> {
    List<GroupMembershipEntity> findAllByUserIdAndStatus(String userId, String status);
    List<GroupMembershipEntity> findAllByGroupIdAndStatus(String groupId, String status);
    Optional<GroupMembershipEntity> findByGroupIdAndUserIdAndStatus(String groupId, String userId, String status);
}
