package vn.springboot.dto.response.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A permission from the fixed catalog. Permissions are seeded (mapped 1:1 to
 * {@code @PreAuthorize} checks in code) — read-only for the admin UI.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {

    private Long id;

    private String name;

    private String description;
}
