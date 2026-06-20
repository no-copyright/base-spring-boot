package vn.springboot.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Port for persisting uploaded files. The default {@link LocalStorageService}
 * writes to the local filesystem; provide another bean (S3/MinIO/...) to swap
 * the backend without touching callers.
 */
public interface StorageService {

    /**
     * Store {@code file} under {@code directory} (e.g. {@code "avatars"}) and return
     * its public URL + metadata. Throws {@code AppException(FILE_EMPTY)} for an empty upload.
     */
    StoredFile store(MultipartFile file, String directory);

    /** Delete a previously stored file by its public URL. No-op for null/blank/external URLs. */
    void delete(String url);
}
