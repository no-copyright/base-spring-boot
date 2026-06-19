package vn.springboot.dto.response.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.springboot.enums.NotificationType;

import java.time.Instant;

/**
 * Notification payload returned by the REST API and pushed over WebSocket/FCM.
 * Carries everything the FE needs to render a production-ready item: a resolved
 * {@code icon}, {@code category}, optional image/deep-link, and read state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {

    /** Null for transient broadcast notifications (not persisted per user). */
    private Long id;

    private NotificationType type;

    /** Grouping key from the type (order / promotion / system / ...). */
    private String category;

    /** Resolved icon key (override or the type default). */
    private String icon;

    private String title;

    private String content;

    private String imageUrl;

    private String linkUrl;

    /** Type-specific payload (parsed from stored JSON). */
    private Object data;

    private boolean read;

    private Instant readAt;

    private Instant createdAt;
}
