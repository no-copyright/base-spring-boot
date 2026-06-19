package vn.springboot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.springboot.entity.notification.NotificationEntity;
import vn.springboot.entity.user.UserEntity;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface NotificationRepository
        extends JpaRepository<NotificationEntity, Long>, JpaSpecificationExecutor<NotificationEntity> {

    Optional<NotificationEntity> findByIdAndRecipient(Long id, UserEntity recipient);

    long countByRecipientAndReadFalse(UserEntity recipient);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.read = true, n.readAt = :now "
            + "WHERE n.recipient = :recipient AND n.read = false")
    int markAllReadByRecipient(@Param("recipient") UserEntity recipient, @Param("now") Instant now);
}
