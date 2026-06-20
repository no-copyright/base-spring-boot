package vn.springboot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.springboot.common.response.ApiResponse;
import vn.springboot.dto.response.file.FileUploadResponse;
import vn.springboot.storage.StoredFile;
import vn.springboot.storage.StorageService;

/**
 * Generic file upload for the authenticated user. Returns a public URL the FE
 * can then attach to whatever resource it is editing (e.g. an avatar). Reusable
 * across features — domain-specific helpers (like avatar) build on this.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        StoredFile stored = storageService.store(file, "uploads");
        return ApiResponse.success(FileUploadResponse.builder()
                .url(stored.url())
                .filename(stored.filename())
                .contentType(stored.contentType())
                .size(stored.size())
                .build());
    }
}
