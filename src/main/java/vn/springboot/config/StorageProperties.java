package vn.springboot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * File-storage settings. The base ships a local-filesystem store; swap in an
 * S3/MinIO adapter later by providing another {@code StorageService} bean.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** Filesystem root where uploads are written (relative to the working dir or absolute). */
    private String location = "uploads";

    /** URL path prefix that serves stored files (see StaticResourceConfig + SecurityConfig). */
    private String publicPath = "/files";

    /**
     * Optional absolute base URL (e.g. a CDN/host) prepended to returned file URLs.
     * Empty -> return root-relative URLs like {@code /files/avatars/xxx.png} and let the FE prepend the host.
     */
    private String baseUrl = "";

    /** Content types accepted as images (used by avatar upload). */
    private List<String> allowedImageTypes = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
}
