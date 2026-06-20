package vn.springboot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.springboot.dto.response.permission.PermissionResponse;
import vn.springboot.mapper.PermissionMapper;
import vn.springboot.repository.PermissionRepository;
import vn.springboot.service.PermissionService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAll() {
        return permissionRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(permissionMapper::toResponse)
                .toList();
    }
}
