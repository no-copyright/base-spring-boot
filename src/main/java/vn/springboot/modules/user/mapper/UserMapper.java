package vn.springboot.modules.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.springboot.modules.user.dto.response.UserResponse;
import vn.springboot.modules.user.entity.Permission;
import vn.springboot.modules.user.entity.Role;
import vn.springboot.modules.user.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps {@link User} to its API representation. Scalar fields (id, username,
 * email, fullName, enabled) are mapped by name; roles and the permissions granted
 * through them are flattened via the helper methods below.
 * MapStruct generates the Spring bean implementation at compile time.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(roleNames(user))")
    @Mapping(target = "permissions", expression = "java(permissionNames(user))")
    UserResponse toResponse(User user);

    default Set<String> roleNames(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    default Set<String> permissionNames(User user) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }
}
