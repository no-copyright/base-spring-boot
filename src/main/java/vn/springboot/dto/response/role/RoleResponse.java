package vn.springboot.dto.response.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * A role with the set of permission codes granted to it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {

    private Long id;

    private String name;

    private String description;

    /** Permission codes granted by this role, e.g. ["USER_READ", "USER_WRITE"]. */
    private Set<String> permissions;
}
