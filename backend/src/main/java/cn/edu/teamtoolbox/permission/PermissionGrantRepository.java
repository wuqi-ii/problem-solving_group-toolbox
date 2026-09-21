package cn.edu.teamtoolbox.permission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionGrantRepository extends JpaRepository<PermissionGrantEntity, String> {
    boolean existsByGroupIdAndUserIdAndPermissionAndRevokedAtIsNull(String groupId, String userId, String permission);
    List<PermissionGrantEntity> findAllByGroupIdAndUserIdInAndRevokedAtIsNull(String groupId, List<String> userIds);
    Optional<PermissionGrantEntity> findByGroupIdAndUserIdAndPermissionAndRevokedAtIsNull(
            String groupId, String userId, String permission);
}
