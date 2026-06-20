package vn.springboot.notification.push;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.springboot.dto.response.notification.NotificationResponse;

import java.util.List;

/**
 * FCM-backed {@link PushSender}: delivers a notification to a batch of device
 * tokens via Firebase Cloud Messaging. Wired by {@code FirebaseConfig} only when
 * {@code app.fcm.enabled=true}; otherwise the no-op {@code LoggingPushSender} is used.
 *
 * <p>The visible title/body go in the FCM {@code notification} block; structured
 * fields (type, category, deep link) go in {@code data} so the mobile app can route on tap.
 */
@Slf4j
@RequiredArgsConstructor
public class FcmPushSender implements PushSender {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public void sendToDevices(List<String> deviceTokens, NotificationResponse notification) {
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            return;
        }

        MulticastMessage.Builder message = MulticastMessage.builder()
                .addAllTokens(deviceTokens)
                .setNotification(Notification.builder()
                        .setTitle(notification.getTitle())
                        .setBody(notification.getContent())
                        .setImage(notification.getImageUrl())
                        .build())
                .putData("type", notification.getType() != null ? notification.getType().name() : "")
                .putData("category", notification.getCategory() != null ? notification.getCategory() : "");
        if (notification.getId() != null) {
            message.putData("id", String.valueOf(notification.getId()));
        }
        if (notification.getLinkUrl() != null) {
            message.putData("linkUrl", notification.getLinkUrl());
        }

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message.build());
            log.debug("FCM push: {} delivered, {} failed", response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException ex) {
            // Never let a push failure break the business transaction that triggered it.
            log.warn("FCM push failed for {} device(s): {}", deviceTokens.size(), ex.getMessage());
        }
    }
}
