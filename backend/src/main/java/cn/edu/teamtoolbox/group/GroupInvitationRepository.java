package cn.edu.teamtoolbox.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitationEntity, String> {
    Optional<GroupInvitationEntity> findByCode(String code);
    boolean existsByCode(String code);
    List<GroupInvitationEntity> findAllByGroupIdOrderByCreatedAtDesc(String groupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from GroupInvitationEntity invitation where invitation.code = :code")
    Optional<GroupInvitationEntity> findByCodeForUpdate(@Param("code") String code);
}
