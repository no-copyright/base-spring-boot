package vn.springboot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.springboot.dto.response.role.RoleResponse;
import vn.springboot.entity.user.PermissionEntity;
import vn.springboot.entity.user.RoleEntity;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps {@link RoleEntity} to {@link RoleResponse}, flattening permissions to
 * their codes.
 */
@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "permissions", expression = "java(permissionNames(role))")
    RoleResponse toResponse(RoleEntity role);

    default Set<String> permissionNames(RoleEntity role) {
        return role.getPermissions().stream()
                .map(PermissionEntity::getName)
                .collect(Collectors.toSet());
    }
}
