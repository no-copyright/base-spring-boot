package vn.springboot.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import vn.springboot.notification.push.FcmPushSender;
import vn.springboot.notification.push.PushSender;

import java.io.IOException;
import java.io.InputStream;

/**
 * Initializes Firebase and the FCM-backed {@link PushSender} — only when
 * {@code app.fcm.enabled=true}. When disabled this whole config is skipped and
 * {@code PushConfig} keeps the no-op logger, so the app runs without credentials.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(FcmProperties.class)
@ConditionalOnProperty(name = "app.fcm.enabled", havingValue = "true")
public class FirebaseConfig {

    private final FcmProperties properties;
    private final ResourceLoader resourceLoader;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        Resource resource = resourceLoader.getResource(properties.getCredentials());
        try (InputStream credentials = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase initialized for FCM push (credentials: {})", properties.getCredentials());
            return app;
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    /** Real push sender; overrides the no-op because PushConfig only wires its fallback when FCM is off. */
    @Bean
    public PushSender fcmPushSender(FirebaseMessaging firebaseMessaging) {
        return new FcmPushSender(firebaseMessaging);
    }
}
