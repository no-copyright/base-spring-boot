package vn.springboot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.springboot.notification.push.LoggingPushSender;
import vn.springboot.notification.push.PushSender;

/**
 * Wires the default no-op {@link PushSender}. When an FCM-backed bean is added
 * (e.g. {@code FcmPushSender}), this fallback steps aside automatically.
 */
@Configuration
public class PushConfig {

    @Bean
    @ConditionalOnMissingBean(PushSender.class)
    public PushSender pushSender() {
        return new LoggingPushSender();
    }
}
