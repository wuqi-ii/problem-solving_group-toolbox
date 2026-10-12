package cn.edu.teamtoolbox.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupFileRepository extends JpaRepository<GroupFileEntity, String> {
    List<GroupFileEntity> findAllByGroupIdAndStatusOrderByCreatedAtDesc(String groupId, String status);
    Optional<GroupFileEntity> findTopByGroupIdAndOriginalNameOrderByVersionNoDesc(String groupId, String originalName);
}
