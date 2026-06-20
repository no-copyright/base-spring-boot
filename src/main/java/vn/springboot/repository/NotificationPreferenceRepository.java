package vn.springboot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.springboot.entity.notification.NotificationPreferenceEntity;
import vn.springboot.enums.NotificationType;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, Long> {

    /** All explicit preferences for a user (types not listed are enabled by default). */
    List<NotificationPreferenceEntity> findByUserId(Long userId);

    /** Single (user, type) preference — used on the emit path to decide delivery. */
    Optional<NotificationPreferenceEntity> findByUserIdAndType(Long userId, NotificationType type);

    /**
     * True when the given channel is explicitly OFF for this (user, type).
     * Returns false when no row exists (default = enabled). Kept as JPQL so the
     * emit path costs one boolean query instead of loading the entity.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END "
            + "FROM NotificationPreferenceEntity p "
            + "WHERE p.userId = :userId AND p.type = :type AND p.inAppEnabled = false")
    boolean isInAppMuted(@Param("userId") Long userId, @Param("type") NotificationType type);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END "
            + "FROM NotificationPreferenceEntity p "
            + "WHERE p.userId = :userId AND p.type = :type AND p.pushEnabled = false")
    boolean isPushMuted(@Param("userId") Long userId, @Param("type") NotificationType type);
}
