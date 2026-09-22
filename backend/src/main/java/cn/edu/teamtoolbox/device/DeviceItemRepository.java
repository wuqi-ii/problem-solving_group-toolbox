package cn.edu.teamtoolbox.device;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceItemRepository extends JpaRepository<DeviceItemEntity, String> {
    List<DeviceItemEntity> findAllByOwnerIdAndStatusOrderByCreatedAtDesc(String ownerId, String status);
}
