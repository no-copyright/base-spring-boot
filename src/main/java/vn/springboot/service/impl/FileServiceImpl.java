package vn.springboot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.springboot.config.StorageProperties;
import vn.springboot.dto.response.file.FileUploadResponse;
import vn.springboot.entity.file.FileEntity;
import vn.springboot.repository.FileRepository;
import vn.springboot.security.SecurityUtils;
import vn.springboot.service.FileService;
import vn.springboot.storage.StoredFile;
import vn.springboot.storage.StorageService;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final int MAX_ORIGINAL_NAME = 255;

    private final StorageService storageService;
    private final FileRepository fileRepository;
    private final StorageProperties storageProperties;

    @Override
    @Transactional
    public FileUploadResponse upload(MultipartFile file, String directory) {
        Long ownerId = SecurityUtils.currentUser().getId();
        StoredFile stored = storageService.store(file, directory);

        FileEntity entity = FileEntity.builder()
                .storageKey(stored.key())
                .originalFilename(truncate(file.getOriginalFilename()))
                .contentType(stored.contentType())
                .sizeBytes(stored.size())
                .ownerId(ownerId)
                .build();
        fileRepository.save(entity);

        return FileUploadResponse.builder()
                .id(entity.getId())
                .url(stored.url())
                .filename(entity.getOriginalFilename())
                .contentType(entity.getContentType())
                .size(entity.getSizeBytes())
                .build();
    }

    @Override
    @Transactional
    public void deleteByUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        String key = extractKey(url);
        if (key != null) {
            fileRepository.findByStorageKey(key).ifPresent(fileRepository::delete);
        }
        storageService.delete(url);
    }

    /** Public URL -> storage key (the part after the public path prefix). */
    private String extractKey(String url) {
        String marker = stripTrailingSlash(storageProperties.getPublicPath()) + "/";
        int idx = url.indexOf(marker);
        return idx == -1 ? null : url.substring(idx + marker.length());
    }

    private static String truncate(String name) {
        if (name == null) {
            return null;
        }
        return name.length() <= MAX_ORIGINAL_NAME ? name : name.substring(0, MAX_ORIGINAL_NAME);
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
