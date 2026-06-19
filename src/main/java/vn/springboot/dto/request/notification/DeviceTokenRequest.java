package vn.springboot.dto.request.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.springboot.enums.DevicePlatform;

/**
 * Registers (or refreshes) a device's push token for the current user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceTokenRequest {

    @NotBlank(message = "Device token is required")
    private String token;

    @NotNull(message = "Platform is required")
    private DevicePlatform platform;
}
