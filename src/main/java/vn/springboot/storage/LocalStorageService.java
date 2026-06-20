package vn.springboot.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.springboot.common.exception.AppException;
import vn.springboot.common.exception.ErrorCode;
import vn.springboot.config.StorageProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local-filesystem {@link StorageService}: writes uploads under
 * {@code app.storage.location} using a random filename (original names are never
 * trusted), and returns a URL served by {@code StaticResourceConfig}.
 */
@Slf4j
@Service
public class LocalStorageService implements StorageService {

    private final Path root;
    private final String publicPath;
    private final String baseUrl;

    public LocalStorageService(StorageProperties properties) {
        this.root = Paths.get(properties.getLocation()).toAbsolutePath().normalize();
        this.publicPath = stripTrailingSlash(properties.getPublicPath());
        this.baseUrl = stripTrailingSlash(properties.getBaseUrl());
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
            log.info("File storage ready at {}", root);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.FILE_STORAGE_FAILED, "Cannot create storage dir: " + root);
        }
    }

    @Override
    public StoredFile store(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }

        String dir = sanitizeDirectory(directory);
        String filename = uniqueFilename(file.getOriginalFilename());
        String relative = dir + "/" + filename;

        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            // Defence in depth — should be impossible since we generate the path.
            throw new AppException(ErrorCode.FILE_STORAGE_FAILED, "Resolved path escapes storage root");
        }

        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.warn("Failed to store upload: {}", ex.getMessage());
            throw new AppException(ErrorCode.FILE_STORAGE_FAILED);
        }

        return new StoredFile(toUrl(relative), relative, filename, file.getContentType(), file.getSize());
    }

    @Override
    public void delete(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        String marker = publicPath + "/";
        int idx = url.indexOf(marker);
        if (idx == -1) {
            return; // not one of ours (external URL)
        }
        String relative = url.substring(idx + marker.length());
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.warn("Failed to delete stored file {}: {}", url, ex.getMessage());
        }
    }

    private String toUrl(String relative) {
        return baseUrl + publicPath + "/" + relative;
    }

    private String uniqueFilename(String originalName) {
        String ext = StringUtils.getFilenameExtension(originalName);
        String base = UUID.randomUUID().toString().replace("-", "");
        return (ext == null || ext.isBlank()) ? base : base + "." + ext.toLowerCase();
    }

    private String sanitizeDirectory(String directory) {
        if (directory == null || directory.isBlank()) {
            return "misc";
        }
        String cleaned = directory.replaceAll("[^a-zA-Z0-9_-]", "");
        return cleaned.isBlank() ? "misc" : cleaned;
    }

    private static String stripTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
