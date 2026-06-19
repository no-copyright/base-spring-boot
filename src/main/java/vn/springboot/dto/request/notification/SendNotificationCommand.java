package vn.springboot.dto.request.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.springboot.enums.NotificationType;

/**
 * Internal command used by backend code to emit a notification. Not bound to an
 * HTTP request — business services build this and call {@code NotificationService}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationCommand {

    private NotificationType type;

    private String title;

    private String content;

    /** Optional icon override; null -> use the type's default icon. */
    private String icon;

    private String imageUrl;

    private String linkUrl;

    /** Arbitrary type-specific payload; serialized to JSON for storage. */
    private Object data;
}
