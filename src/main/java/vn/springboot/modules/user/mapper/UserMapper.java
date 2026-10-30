package vn.springboot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.springboot.dto.response.user.UserResponse;
import vn.springboot.entity.user.PermissionEntity;
import vn.springboot.entity.user.RoleEntity;
import vn.springboot.entity.user.UserEntity;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps {@link UserEntity} to its API representation. Scalar fields (id, username,
 * email, fullName, enabled) are mapped by name; roles and the permissions granted
 * through them are flattened via the helper methods below.
 * MapStruct generates the Spring bean implementation at compile time.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(roleNames(user))")
    @Mapping(target = "permissions", expression = "java(permissionNames(user))")
    UserResponse toResponse(UserEntity user);

    default Set<String> roleNames(UserEntity user) {
        return user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());
    }

    default Set<String> permissionNames(UserEntity user) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(PermissionEntity::getName)
                .collect(Collectors.toSet());
    }
}
