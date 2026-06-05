package vn.springboot.mapper;

import org.springframework.stereotype.Component;
import vn.springboot.dto.response.user.UserResponse;
import vn.springboot.entity.user.PermissionEntity;
import vn.springboot.entity.user.RoleEntity;
import vn.springboot.entity.user.UserEntity;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(UserEntity user) {
        Set<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(PermissionEntity::getName)
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .enabled(user.isEnabled())
                .roles(roles)
                .permissions(permissions)
                .build();
    }
}
