package vn.springboot.dto.response.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.springboot.enums.NotificationType;

/**
 * One row of the user's notification settings screen: the type's catalog info
 * (category + icon) plus the effective per-channel toggles. The FE renders the
 * whole settings page from the list of these — no separate "type catalog" call.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {

    private NotificationType type;

    /** Grouping key (order / promotion / system / ...) so the FE can section the list. */
    private String category;

    /** Default icon key for the type. */
    private String icon;

    /** In-app notification centre + real-time WebSocket delivery. */
    private boolean inAppEnabled;

    /** FCM push to registered devices. */
    private boolean pushEnabled;
}
