package vn.springboot.service;

import vn.springboot.dto.request.role.CreateRoleRequest;
import vn.springboot.dto.request.role.UpdateRoleRequest;
import vn.springboot.dto.response.role.RoleResponse;

import java.util.List;

/**
 * Admin role management: list/read roles, create a role with permissions, update
 * a role's permission set, delete a role. Built-in roles (ADMIN/USER) are guarded.
 */
public interface RoleService {

    List<RoleResponse> getAll();

    RoleResponse getById(Long id);

    RoleResponse create(CreateRoleRequest request);

    RoleResponse update(Long id, UpdateRoleRequest request);

    void delete(Long id);
}
