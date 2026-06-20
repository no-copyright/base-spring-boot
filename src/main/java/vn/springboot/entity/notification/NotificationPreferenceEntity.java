package vn.springboot.entity.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.springboot.common.entity.BaseEntity;
import vn.springboot.enums.NotificationType;

/**
 * Per-user, per-type notification opt-out. One row = one user disabling a
 * channel for a given {@link NotificationType}. Absence of a row means
 * "enabled" (opt-out model), so we only persist explicit user choices.
 *
 * <p>Per the project convention, the owning user is stored as a plain
 * {@code user_id} column (no JPA relationship mapping) — see CLAUDE.md §4.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notification_preferences",
        // Unique (user_id, type) doubles as the index for "load this user's prefs"
        // and "(user, type)" lookups — no extra single-column index needed.
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notif_pref_user_type", columnNames = {"user_id", "type"}))
public class NotificationPreferenceEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    /** Show in the in-app notification centre + real-time WebSocket push. */
    @Builder.Default
    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    /** Deliver as an FCM push to the user's registered devices. */
    @Builder.Default
    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = true;
}
