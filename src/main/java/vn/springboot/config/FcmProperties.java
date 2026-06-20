package vn.springboot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FCM (Firebase Cloud Messaging) settings. Disabled by default so the project
 * boots out-of-the-box with the no-op {@code LoggingPushSender}; flip
 * {@code app.fcm.enabled=true} and point {@code credentials} at a service-account
 * JSON to enable real push delivery.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.fcm")
public class FcmProperties {

    /** Master switch for real FCM delivery. */
    private boolean enabled = false;

    /**
     * Spring resource location of the Firebase service-account JSON.
     * Examples: {@code classpath:firebase-service-account.json},
     * {@code file:/etc/secrets/firebase.json}.
     */
    private String credentials = "classpath:firebase-service-account.json";
}
