package vn.springboot.dto.request.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Create a role and attach permissions (by id). Permissions come from the fixed
 * catalog ({@code GET /api/permissions}).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name must be at most 50 characters")
    private String name;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    /** Permission ids to grant; empty/null = role with no permissions. */
    private Set<Long> permissionIds;
}
