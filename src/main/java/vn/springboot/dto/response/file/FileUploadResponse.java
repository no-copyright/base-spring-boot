package vn.springboot.dto.response.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a file upload: the public {@code url} the FE stores/renders, plus metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponse {

    private String url;

    private String filename;

    private String contentType;

    private long size;
}
