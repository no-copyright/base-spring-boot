package vn.springboot.dto.request.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Set the roles of a user (admin action). Replaces the user's current role set
 * with the given role ids.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignRolesRequest {

    @NotEmpty(message = "At least one role id is required")
    private Set<Long> roleIds;
}
