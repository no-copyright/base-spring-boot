package vn.springboot.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import vn.springboot.common.exception.AppException;
import vn.springboot.common.exception.ErrorCode;
import vn.springboot.config.StorageProperties;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService service;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setLocation(tempDir.toString());
        properties.setPublicPath("/files");
        properties.setBaseUrl("");
        service = new LocalStorageService(properties);
        service.init();
    }

    @Test
    void store_writesFileAndReturnsPublicUrl() {
        var file = new MockMultipartFile("file", "avatar.PNG", "image/png", "data".getBytes());

        StoredFile stored = service.store(file, "avatars");

        assertTrue(stored.url().startsWith("/files/avatars/"));
        assertTrue(stored.url().endsWith(".png"), "extension should be normalized to lowercase");
        assertEquals(4, stored.size());
        assertTrue(Files.exists(tempDir.resolve("avatars").resolve(stored.filename())));
    }

    @Test
    void store_emptyFile_throwsFileEmpty() {
        var empty = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);

        AppException ex = assertThrows(AppException.class, () -> service.store(empty, "avatars"));
        assertEquals(ErrorCode.FILE_EMPTY, ex.getErrorCode());
    }

    @Test
    void delete_removesStoredFile() {
        var file = new MockMultipartFile("file", "a.png", "image/png", "data".getBytes());
        StoredFile stored = service.store(file, "avatars");
        assertTrue(Files.exists(tempDir.resolve("avatars").resolve(stored.filename())));

        service.delete(stored.url());

        assertFalse(Files.exists(tempDir.resolve("avatars").resolve(stored.filename())));
    }
}
