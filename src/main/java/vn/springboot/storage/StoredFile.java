package vn.springboot.storage;

/**
 * Result of storing a file: the public {@code url} to reach it plus basic metadata.
 *
 * @param url         public URL (root-relative, or absolute when app.storage.base-url is set)
 * @param key         storage key = path relative to the storage root (e.g. {@code avatars/uuid.png})
 * @param filename    generated unique filename on disk
 * @param contentType MIME type reported by the upload
 * @param size        size in bytes
 */
public record StoredFile(String url, String key, String filename, String contentType, long size) {
}
