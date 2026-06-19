package vn.springboot.notification.push;

import vn.springboot.dto.response.notification.NotificationResponse;

import java.util.List;

/**
 * Port for delivering a push notification to a user's devices (FCM/APNs).
 * Implemented later by an FCM adapter; for now {@link LoggingPushSender} is a
 * no-op so the rest of the pipeline can be wired and tested.
 */
public interface PushSender {

    void sendToDevices(List<String> deviceTokens, NotificationResponse notification);
}
