package vn.springboot.dto.request.user;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Self-service profile update for the current user. All fields optional — only
 * non-null values are applied. Avatar can be set here (with a URL from a prior
 * upload) or via the dedicated multipart endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @Size(max = 500, message = "Avatar URL must be at most 500 characters")
    private String avatarUrl;
}
