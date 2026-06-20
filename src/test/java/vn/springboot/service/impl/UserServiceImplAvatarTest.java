package vn.springboot.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.springboot.common.exception.AppException;
import vn.springboot.common.exception.ErrorCode;
import vn.springboot.config.StorageProperties;
import vn.springboot.dto.response.file.FileUploadResponse;
import vn.springboot.dto.response.user.UserResponse;
import vn.springboot.entity.user.UserEntity;
import vn.springboot.mapper.UserMapper;
import vn.springboot.repository.UserRepository;
import vn.springboot.security.CustomUserDetails;
import vn.springboot.service.FileService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplAvatarTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock FileService fileService;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, userMapper, fileService, new StorageProperties());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private UserEntity authenticatedUser() {
        UserEntity user = UserEntity.builder().username("bob").avatarUrl("/files/avatars/old.png").build();
        user.setId(1L);
        var auth = new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return user;
    }

    @Test
    void updateMyAvatar_storesSetsUrlAndDeletesOld() {
        UserEntity user = authenticatedUser();
        var file = new MockMultipartFile("file", "new.png", "image/png", "img".getBytes());
        UserResponse response = UserResponse.builder().id(1L).avatarUrl("/files/avatars/new.png").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(fileService.upload(file, "avatars"))
                .thenReturn(FileUploadResponse.builder().id(9L).url("/files/avatars/new.png").build());
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = service.updateMyAvatar(file);

        assertEquals("/files/avatars/new.png", result.getAvatarUrl());
        assertEquals("/files/avatars/new.png", user.getAvatarUrl());
        verify(fileService).deleteByUrl("/files/avatars/old.png");
    }

    @Test
    void updateMyAvatar_nonImage_throwsInvalidType() {
        var file = new MockMultipartFile("file", "note.txt", "text/plain", "hi".getBytes());

        AppException ex = assertThrows(AppException.class, () -> service.updateMyAvatar(file));

        assertEquals(ErrorCode.INVALID_FILE_TYPE, ex.getErrorCode());
        verify(fileService, never()).upload(any(), eq("avatars"));
    }
}
