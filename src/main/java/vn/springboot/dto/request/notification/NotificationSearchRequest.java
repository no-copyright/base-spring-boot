package vn.springboot.dto.request.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.springboot.enums.NotificationType;

/**
 * Query params for listing the current user's notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSearchRequest {

    /** Filter by read state; null = both. */
    private Boolean read;

    /** Filter by type; null = all types. */
    private NotificationType type;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    @Builder.Default
    private String sortDirection = "DESC";
}
