package vn.springboot.entity.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.springboot.common.entity.BaseEntity;
import vn.springboot.entity.user.UserEntity;
import vn.springboot.enums.NotificationType;

import java.time.Instant;

/**
 * A persisted, per-user notification (the "notification center" record).
 * Created by {@code NotificationService}, delivered in real time over STOMP and
 * (later) FCM, and listed/marked-read through the REST API.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notifications", indexes = {
        // Fast unread-count and per-user listing.
        @Index(name = "idx_notification_recipient_read", columnList = "recipient_id, is_read"),
        @Index(name = "idx_notification_recipient_created", columnList = "recipient_id, created_at")
})
public class NotificationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserEntity recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", length = 1000)
    private String content;

    /** Optional icon override; when null the FE should use {@link NotificationType#getIcon()}. */
    @Column(name = "icon", length = 100)
    private String icon;

    /** Optional rich image (e.g. promotion banner). */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Deep link / action URL the FE navigates to on tap. */
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    /** Arbitrary type-specific payload, stored as a JSON string. */
    @Lob
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;

    /** Effective icon: explicit override, else the type's default. */
    public String resolveIcon() {
        if (icon != null && !icon.isBlank()) {
            return icon;
        }
        return type != null ? type.getIcon() : null;
    }
}
