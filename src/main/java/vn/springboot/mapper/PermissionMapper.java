package vn.springboot.mapper;

import org.mapstruct.Mapper;
import vn.springboot.dto.response.permission.PermissionResponse;
import vn.springboot.entity.user.PermissionEntity;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionResponse toResponse(PermissionEntity permission);
}
