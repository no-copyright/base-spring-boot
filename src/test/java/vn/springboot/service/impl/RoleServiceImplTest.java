package vn.springboot.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.springboot.common.exception.AppException;
import vn.springboot.common.exception.ErrorCode;
import vn.springboot.dto.request.role.CreateRoleRequest;
import vn.springboot.dto.request.role.UpdateRoleRequest;
import vn.springboot.entity.user.PermissionEntity;
import vn.springboot.entity.user.RoleEntity;
import vn.springboot.mapper.RoleMapper;
import vn.springboot.repository.PermissionRepository;
import vn.springboot.repository.RoleRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock RoleRepository roleRepository;
    @Mock PermissionRepository permissionRepository;
    @Mock RoleMapper roleMapper;

    @InjectMocks RoleServiceImpl service;

    private PermissionEntity permission(long id, String name) {
        PermissionEntity p = PermissionEntity.builder().name(name).build();
        p.setId(id);
        return p;
    }

    @Test
    void create_attachesPermissions() {
        var request = CreateRoleRequest.builder()
                .name("MANAGER").description("Quản lý").permissionIds(Set.of(1L, 2L)).build();
        when(roleRepository.existsByName("MANAGER")).thenReturn(false);
        when(permissionRepository.findAllById(Set.of(1L, 2L)))
                .thenReturn(List.of(permission(1L, "USER_READ"), permission(2L, "USER_WRITE")));
        when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(request);

        ArgumentCaptor<RoleEntity> captor = ArgumentCaptor.forClass(RoleEntity.class);
        verify(roleRepository).save(captor.capture());
        RoleEntity saved = captor.getValue();
        assertEquals("MANAGER", saved.getName());
        assertEquals(2, saved.getPermissions().size());
    }

    @Test
    void create_duplicateName_throws() {
        when(roleRepository.existsByName("ADMIN")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () ->
                service.create(CreateRoleRequest.builder().name("ADMIN").build()));

        assertEquals(ErrorCode.ROLE_NAME_EXISTED, ex.getErrorCode());
        verify(roleRepository, never()).save(any());
    }

    @Test
    void create_unknownPermissionId_throws() {
        when(roleRepository.existsByName("MANAGER")).thenReturn(false);
        when(permissionRepository.findAllById(Set.of(1L, 999L)))
                .thenReturn(List.of(permission(1L, "USER_READ"))); // 999 missing

        AppException ex = assertThrows(AppException.class, () ->
                service.create(CreateRoleRequest.builder().name("MANAGER").permissionIds(Set.of(1L, 999L)).build()));

        assertEquals(ErrorCode.PERMISSION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void update_adminRole_isProtected() {
        RoleEntity admin = RoleEntity.builder().name("ADMIN").build();
        admin.setId(1L);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(admin));

        AppException ex = assertThrows(AppException.class, () ->
                service.update(1L, UpdateRoleRequest.builder().description("x").build()));

        assertEquals(ErrorCode.ROLE_PROTECTED, ex.getErrorCode());
        verify(roleRepository, never()).save(any());
    }

    @Test
    void delete_userRole_isProtected() {
        RoleEntity user = RoleEntity.builder().name("USER").build();
        user.setId(2L);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(user));

        AppException ex = assertThrows(AppException.class, () -> service.delete(2L));

        assertEquals(ErrorCode.ROLE_PROTECTED, ex.getErrorCode());
        verify(roleRepository, never()).delete(any());
    }
}
