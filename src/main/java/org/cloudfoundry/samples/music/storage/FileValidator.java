package org.cloudfoundry.samples.music.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enforces security guardrails on every uploaded file before it reaches storage:
 * - Whitelist of allowed MIME types
 * - Whitelist of allowed file extensions (double-checked server-side)
 * - Maximum file size
 * - Filename sanitization (no path traversal, no control characters)
 */
@Component
public class FileValidator {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "text/plain", "text/csv", "text/xml",
            "application/json", "application/pdf",
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "audio/mpeg", "audio/wav", "audio/flac", "audio/ogg"
    );

    private final Set<String> allowedExtensions;
    private final long maxFileSizeBytes;

    public FileValidator(
            @Value("${storage.allowed-extensions:txt,csv,json,xml,pdf,jpg,jpeg,png,gif,webp,mp3,wav,flac}")
            String allowedExtensionsCsv,
            @Value("${storage.max-file-bytes:10485760}") long maxFileSizeBytes) {
        this.allowedExtensions = Arrays.stream(allowedExtensionsCsv.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided or file is empty");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "File exceeds maximum allowed size of " + (maxFileSizeBytes / 1024 / 1024) + " MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isMimeTypeAllowed(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "File type not permitted: " + contentType);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        if (!allowedExtensions.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "File extension not permitted: " + extension);
        }
    }

    /**
     * Sanitizes a filename for safe use as part of an object storage key.
     * Strips path separators, null bytes, and control characters.
     * Retains only alphanumerics, dots, dashes, and underscores.
     */
    public String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload";
        }
        String name = originalFilename
                .replaceAll("[/\\\\]", "")
                .replaceAll("\\.\\.", "")
                .replaceAll("[\\x00-\\x1F\\x7F]", "")
                .replaceAll("[^a-zA-Z0-9._\\-]", "_");
        return name.isBlank() ? "upload" : name;
    }

    private boolean isMimeTypeAllowed(String contentType) {
        String base = contentType.contains(";") ? contentType.substring(0, contentType.indexOf(';')).trim() : contentType.trim();
        return ALLOWED_MIME_TYPES.contains(base.toLowerCase()) || base.toLowerCase().startsWith("text/");
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
