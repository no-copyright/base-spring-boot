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
import vn.springboot.service.FileService;

/**
 * Generic file upload for the authenticated user: stores the bytes, registers
 * the file in the {@code files} table, and returns its id + public URL. The FE
 * attaches the URL (or id) to whatever resource it is editing. Reusable across
 * features — domain helpers (like avatar) build on the same {@code FileService}.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(fileService.upload(file, "misc"));
    }
}
