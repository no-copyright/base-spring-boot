package vn.springboot.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.springboot.config.StorageProperties;
import vn.springboot.dto.response.file.FileUploadResponse;
import vn.springboot.entity.file.FileEntity;
import vn.springboot.entity.user.UserEntity;
import vn.springboot.repository.FileRepository;
import vn.springboot.security.CustomUserDetails;
import vn.springboot.storage.StorageService;
import vn.springboot.storage.StoredFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock StorageService storageService;
    @Mock FileRepository fileRepository;

    private FileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FileServiceImpl(storageService, fileRepository, new StorageProperties());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(long userId) {
        UserEntity user = UserEntity.builder().username("bob").build();
        user.setId(userId);
        var auth = new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void upload_storesPersistsMetadataAndReturnsIdUrl() {
        authenticate(1L);
        var file = new MockMultipartFile("file", "photo.png", "image/png", "data".getBytes());
        when(storageService.store(file, "misc"))
                .thenReturn(new StoredFile("/files/misc/abc.png", "misc/abc.png", "abc.png", "image/png", 4));
        when(fileRepository.save(org.mockito.ArgumentMatchers.<FileEntity>any())).thenAnswer(inv -> {
            FileEntity e = inv.getArgument(0);
            e.setId(9L);
            return e;
        });

        FileUploadResponse result = service.upload(file, "misc");

        assertEquals(9L, result.getId());
        assertEquals("/files/misc/abc.png", result.getUrl());

        ArgumentCaptor<FileEntity> captor = ArgumentCaptor.forClass(FileEntity.class);
        verify(fileRepository).save(captor.capture());
        FileEntity saved = captor.getValue();
        assertEquals("misc/abc.png", saved.getStorageKey());
        assertEquals("photo.png", saved.getOriginalFilename());
        assertEquals(1L, saved.getOwnerId());
        assertEquals(4, saved.getSizeBytes());
    }

    @Test
    void deleteByUrl_removesRowAndBytes() {
        String url = "/files/avatars/old.png";
        when(fileRepository.findByStorageKey("avatars/old.png"))
                .thenReturn(Optional.of(new FileEntity()));

        service.deleteByUrl(url);

        verify(fileRepository).delete(org.mockito.ArgumentMatchers.<FileEntity>any());
        verify(storageService).delete(url);
    }

    @Test
    void deleteByUrl_blank_isNoop() {
        service.deleteByUrl("  ");

        verifyNoInteractions(fileRepository);
        verify(storageService, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }
}
