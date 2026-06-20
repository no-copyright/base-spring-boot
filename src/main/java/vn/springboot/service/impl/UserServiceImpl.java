package vn.springboot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.springboot.common.exception.AppException;
import vn.springboot.common.exception.ErrorCode;
import vn.springboot.config.StorageProperties;
import vn.springboot.dto.request.user.UpdateProfileRequest;
import vn.springboot.dto.request.user.UserSearchRequest;
import vn.springboot.dto.response.PageResponse;
import vn.springboot.dto.response.user.UserResponse;
import vn.springboot.entity.user.UserEntity;
import vn.springboot.mapper.UserMapper;
import vn.springboot.dto.response.file.FileUploadResponse;
import vn.springboot.repository.UserRepository;
import vn.springboot.repository.specification.UserSpecification;
import vn.springboot.security.SecurityUtils;
import vn.springboot.service.FileService;
import vn.springboot.service.UserService;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** Whitelisted sortable columns — guards against PropertyReferenceException (500) from arbitrary input. */
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "username", "email", "fullName", "createdAt");
    private static final String DEFAULT_SORT_FIELD = "id";
    private static final int MAX_PAGE_SIZE = 100;

    private static final String AVATAR_DIR = "avatars";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final FileService fileService;
    private final StorageProperties storageProperties;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(UserSearchRequest request) {
        Pageable pageable = PageRequest.of(
                Math.max(0, request.getPage()),
                Math.clamp(request.getSize(), 1, MAX_PAGE_SIZE),
                resolveSort(request));
        Specification<UserEntity> specification = UserSpecification.build(request);

        Page<UserEntity> userPage = userRepository.findAll(specification, pageable);

        List<UserResponse> content = userPage.getContent().stream()
                .map(userMapper::toResponse)
                .toList();

        return PageResponse.<UserResponse>builder()
                .content(content)
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        return userMapper.toResponse(currentManagedUser());
    }

    @Override
    @Transactional
    public UserResponse updateMyProfile(UpdateProfileRequest request) {
        UserEntity me = currentManagedUser();
        if (request.getFullName() != null) {
            me.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null) {
            me.setAvatarUrl(request.getAvatarUrl());
        }
        return userMapper.toResponse(userRepository.save(me));
    }

    @Override
    @Transactional
    public UserResponse updateMyAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }
        if (!storageProperties.getAllowedImageTypes().contains(file.getContentType())) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }

        UserEntity me = currentManagedUser();
        String previousAvatar = me.getAvatarUrl();

        FileUploadResponse uploaded = fileService.upload(file, AVATAR_DIR);
        me.setAvatarUrl(uploaded.getUrl());
        UserResponse response = userMapper.toResponse(userRepository.save(me));

        // Best-effort cleanup of the replaced image (registry row + bytes).
        fileService.deleteByUrl(previousAvatar);
        return response;
    }

    /** The current user reloaded as a managed entity (so changes persist on save). */
    private UserEntity currentManagedUser() {
        Long id = SecurityUtils.currentUser().getId();
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Sort resolveSort(UserSearchRequest request) {
        String field = SORTABLE_FIELDS.contains(request.getSortBy()) ? request.getSortBy() : DEFAULT_SORT_FIELD;
        Sort.Direction direction = "DESC".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}
