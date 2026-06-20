package vn.springboot.dto.request.role;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Update a role's description and/or its permission set. The role {@code name}
 * is immutable (it backs {@code hasRole(...)} checks). Only non-null fields apply;
 * sending {@code permissionIds} REPLACES the role's permissions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRoleRequest {

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    /** When provided, replaces the role's permissions; null = leave unchanged. */
    private Set<Long> permissionIds;
}
