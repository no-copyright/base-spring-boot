package vn.springboot.entity.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.springboot.common.entity.BaseEntity;
import vn.springboot.entity.user.UserEntity;
import vn.springboot.enums.DevicePlatform;

/**
 * A device's push-notification registration token (FCM/APNs). Groundwork for
 * push delivery — the token is captured now; the actual push sender is wired
 * later (see {@code PushSender}).
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "device_tokens")
public class DeviceTokenEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** FCM/APNs registration token. */
    @Column(name = "token", unique = true, nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private DevicePlatform platform;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}
