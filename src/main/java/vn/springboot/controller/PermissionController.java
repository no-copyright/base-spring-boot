package vn.springboot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.springboot.common.response.ApiResponse;
import vn.springboot.dto.response.permission.PermissionResponse;
import vn.springboot.service.PermissionService;

import java.util.List;

/**
 * Read-only catalog of permissions (seeded, code-bound) — the admin UI uses this
 * to pick which permissions to grant a role.
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ApiResponse<List<PermissionResponse>> list() {
        return ApiResponse.success(permissionService.getAll());
    }
}
