package cn.edu.teamtoolbox.transfer;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TemporaryTransferRepository extends JpaRepository<TemporaryTransferEntity, String> {
    List<TemporaryTransferEntity> findAllByOwnerIdOrderByCreatedAtDesc(String ownerId);
    List<TemporaryTransferEntity> findAllByStatusAndExpiresAtBefore(String status, Instant expiresAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TemporaryTransferEntity t where t.pickupCodeHash = :hash")
    Optional<TemporaryTransferEntity> findByCodeForUpdate(@Param("hash") String hash);

    Optional<TemporaryTransferEntity> findByPickupCodeHash(String hash);
}
