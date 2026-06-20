package vn.springboot.service;

import vn.springboot.dto.request.notification.DeviceTokenRequest;
import vn.springboot.dto.request.notification.NotificationSearchRequest;
import vn.springboot.dto.request.notification.SendNotificationCommand;
import vn.springboot.dto.request.notification.UpdateNotificationPreferencesRequest;
import vn.springboot.dto.response.PageResponse;
import vn.springboot.dto.response.notification.NotificationPreferenceResponse;
import vn.springboot.dto.response.notification.NotificationResponse;

import java.util.List;

/**
 * Notification facade: emits notifications (persist + real-time WebSocket push
 * + future FCM), and serves the per-user notification centre (list, unread
 * count, mark-read, delete) plus device-token registration.
 *
 * <p>Concrete business triggers (promotion, order, shipping, ...) are not wired
 * yet — this is the reusable transport other services will call into.
 */
public interface NotificationService {

    // --- Emit -------------------------------------------------------------

    /**
     * Persist a notification for one user and deliver it in real time, honouring
     * the user's per-type preferences: in-app delivery is skipped (and the row not
     * persisted) when muted in-app, push is skipped when muted for push.
     * Returns {@code null} when the type is fully muted for the user.
     */
    NotificationResponse notifyUser(String username, SendNotificationCommand command);

    /** Broadcast a transient notification to every connected client (not persisted). */
    void broadcast(SendNotificationCommand command);

    // --- Notification centre (current user) -------------------------------

    PageResponse<NotificationResponse> getMyNotifications(NotificationSearchRequest request);

    long countMyUnread();

    void markAsRead(Long id);

    void markAllAsRead();

    void delete(Long id);

    // --- Per-type preferences (current user) ------------------------------

    /** Effective settings for every notification type (catalog + toggles). */
    List<NotificationPreferenceResponse> getMyPreferences();

    /** Upsert the current user's toggles for the supplied types; returns the full set. */
    List<NotificationPreferenceResponse> updateMyPreferences(UpdateNotificationPreferencesRequest request);

    // --- Device tokens (FCM groundwork) -----------------------------------

    void registerDeviceToken(DeviceTokenRequest request);

    void unregisterDeviceToken(String token);
}
