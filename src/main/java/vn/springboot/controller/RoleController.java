package vn.springboot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.springboot.common.response.ApiResponse;
import vn.springboot.dto.request.role.CreateRoleRequest;
import vn.springboot.dto.request.role.UpdateRoleRequest;
import vn.springboot.dto.response.role.RoleResponse;
import vn.springboot.service.RoleService;

import java.util.List;

/**
 * Admin role management (RBAC). Read with {@code ROLE_READ}; create/update/delete
 * with {@code ROLE_WRITE}. Built-in roles (ADMIN/USER) are protected server-side.
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.success(roleService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ApiResponse<RoleResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(roleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public ApiResponse<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.success("Role created", roleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public ApiResponse<RoleResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return ApiResponse.success("Role updated", roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success("Role deleted", null);
    }
}
