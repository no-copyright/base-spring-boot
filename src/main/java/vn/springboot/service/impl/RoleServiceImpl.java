package vn.springboot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.springboot.common.exception.AppException;
import vn.springboot.common.exception.ErrorCode;
import vn.springboot.dto.request.role.CreateRoleRequest;
import vn.springboot.dto.request.role.UpdateRoleRequest;
import vn.springboot.dto.response.role.RoleResponse;
import vn.springboot.entity.user.PermissionEntity;
import vn.springboot.entity.user.RoleEntity;
import vn.springboot.mapper.RoleMapper;
import vn.springboot.repository.PermissionRepository;
import vn.springboot.repository.RoleRepository;
import vn.springboot.service.RoleService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    /** ADMIN must keep full power; never editable. */
    private static final Set<String> NON_EDITABLE_ROLES = Set.of("ADMIN");
    /** ADMIN + the default USER role must always exist. */
    private static final Set<String> NON_DELETABLE_ROLES = Set.of("ADMIN", "USER");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAll() {
        return roleRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        return roleMapper.toResponse(findRole(id));
    }

    @Override
    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        String name = request.getName().trim();
        if (roleRepository.existsByName(name)) {
            throw new AppException(ErrorCode.ROLE_NAME_EXISTED);
        }
        RoleEntity role = RoleEntity.builder()
                .name(name)
                .description(request.getDescription())
                .permissions(resolvePermissions(request.getPermissionIds()))
                .build();
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, UpdateRoleRequest request) {
        RoleEntity role = findRole(id);
        if (NON_EDITABLE_ROLES.contains(role.getName())) {
            throw new AppException(ErrorCode.ROLE_PROTECTED);
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getPermissionIds() != null) {
            role.setPermissions(resolvePermissions(request.getPermissionIds()));
        }
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        RoleEntity role = findRole(id);
        if (NON_DELETABLE_ROLES.contains(role.getName())) {
            throw new AppException(ErrorCode.ROLE_PROTECTED);
        }
        // users_roles / roles_permissions rows are removed by ON DELETE CASCADE (see V1 migration).
        roleRepository.delete(role);
    }

    private RoleEntity findRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    }

    /** Resolve permission ids to entities; every id must exist. */
    private Set<PermissionEntity> resolvePermissions(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        List<PermissionEntity> found = permissionRepository.findAllById(permissionIds);
        if (found.size() != permissionIds.size()) {
            throw new AppException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        return new HashSet<>(found);
    }
}
