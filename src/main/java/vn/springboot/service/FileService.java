package vn.springboot.service;

import org.springframework.web.multipart.MultipartFile;
import vn.springboot.dto.response.file.FileUploadResponse;

/**
 * Orchestrates file uploads: persists the bytes via the storage backend AND
 * records metadata in the {@code files} registry (ownership, audit, cleanup).
 * Features should upload through here rather than calling the low-level
 * {@code StorageService} directly.
 */
public interface FileService {

    /** Store {@code file} under {@code directory} and register it for the current user. */
    FileUploadResponse upload(MultipartFile file, String directory);

    /** Delete a file (registry row + stored bytes) by its public URL. No-op if unknown/blank. */
    void deleteByUrl(String url);
}
