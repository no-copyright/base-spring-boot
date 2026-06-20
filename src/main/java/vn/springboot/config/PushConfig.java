package vn.springboot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.springboot.notification.push.LoggingPushSender;
import vn.springboot.notification.push.PushSender;

/**
 * Wires the default no-op {@link PushSender} whenever FCM is off
 * ({@code app.fcm.enabled} unset or false). When FCM is enabled,
 * {@code FirebaseConfig} provides the real {@code FcmPushSender} instead.
 */
@Configuration
public class PushConfig {

    @Bean
    @ConditionalOnProperty(name = "app.fcm.enabled", havingValue = "false", matchIfMissing = true)
    public PushSender pushSender() {
        return new LoggingPushSender();
    }
}
