package vn.springboot.dto.request.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.springboot.enums.NotificationType;

import java.util.List;

/**
 * Bulk update of the current user's notification settings — the "Save" action
 * of the settings screen. Each item sets both channels for one type; types not
 * included are left unchanged.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateNotificationPreferencesRequest {

    @NotEmpty(message = "At least one preference is required")
    @Valid
    private List<Item> preferences;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {

        @NotNull(message = "Type is required")
        private NotificationType type;

        @NotNull(message = "inAppEnabled is required")
        private Boolean inAppEnabled;

        @NotNull(message = "pushEnabled is required")
        private Boolean pushEnabled;
    }
}
