package vn.springboot.entity.file;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.springboot.common.entity.BaseEntity;

/**
 * Registry row for one uploaded file. The bytes live in the storage backend;
 * this tracks metadata for audit, ownership and cleanup. Other features should
 * reference {@code id} (or store the derived URL) rather than re-uploading.
 *
 * <p>{@code ownerId} is a plain column — no JPA relationship mapping (CLAUDE.md §4).
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "files", indexes = @Index(name = "idx_files_owner", columnList = "owner_id"))
public class FileEntity extends BaseEntity {

    /** Path relative to the storage root, e.g. {@code avatars/uuid.png}. Unique. */
    @Column(name = "storage_key", nullable = false, length = 500, unique = true)
    private String storageKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Builder.Default
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes = 0;

    /** Uploader's user id (nullable for system-generated files). */
    @Column(name = "owner_id")
    private Long ownerId;
}
