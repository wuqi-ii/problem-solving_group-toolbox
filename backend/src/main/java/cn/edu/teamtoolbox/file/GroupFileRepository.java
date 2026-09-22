package cn.edu.teamtoolbox.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupFileRepository extends JpaRepository<GroupFileEntity, String> {
    List<GroupFileEntity> findAllByGroupIdAndStatusOrderByCreatedAtDesc(String groupId, String status);
}
