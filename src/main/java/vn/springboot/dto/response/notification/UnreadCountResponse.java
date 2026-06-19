package vn.springboot.dto.response.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unread notification count for the current user — used to render the badge.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponse {
    private long count;
}
