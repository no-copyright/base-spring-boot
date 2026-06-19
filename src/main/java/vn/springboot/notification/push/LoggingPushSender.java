package vn.springboot.notification.push;

import lombok.extern.slf4j.Slf4j;
import vn.springboot.dto.response.notification.NotificationResponse;

import java.util.List;

/**
 * Default {@link PushSender}: logs instead of sending. Registered only when no
 * other {@code PushSender} bean exists (see {@code PushConfig}); provide an
 * FCM-backed implementation to take over real delivery.
 */
@Slf4j
public class LoggingPushSender implements PushSender {

    @Override
    public void sendToDevices(List<String> deviceTokens, NotificationResponse notification) {
        log.info("[push:noop] would push '{}' to {} device(s); wire FCM to enable real delivery",
                notification.getType(), deviceTokens.size());
    }
}
