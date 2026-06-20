package vn.springboot.service;

import vn.springboot.dto.response.permission.PermissionResponse;

import java.util.List;

/**
 * Read-only access to the fixed permission catalog (seeded, code-bound).
 */
public interface PermissionService {

    List<PermissionResponse> getAll();
}
