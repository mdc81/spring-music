package org.cloudfoundry.samples.music.web;

import org.cloudfoundry.samples.music.storage.StorageService;
import org.cloudfoundry.samples.music.storage.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Provides upload, listing, download, and delete endpoints for ECS object storage.
 * Only active when the 'ecs' Spring profile is enabled (i.e. an ECS bucket is bound).
 *
 * Downloads are proxied through the server — ECS credentials are never exposed to clients.
 * Content-Disposition is set to 'attachment' on all downloads to prevent browser execution.
 */
@RestController
@RequestMapping("/storage")
@Profile("ecs")
public class StorageController {

    private static final Logger logger = LoggerFactory.getLogger(StorageController.class);
    private static final int STREAM_BUFFER_SIZE = 8192;

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StoredFile upload(@RequestParam("file") MultipartFile file) {
        logger.info("General upload request: originalFilename={}, size={}", file.getOriginalFilename(), file.getSize());
        return storageService.upload(file);
    }

    @PostMapping(value = "/albums/{albumId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StoredFile uploadForAlbum(@PathVariable String albumId,
                                     @RequestParam("file") MultipartFile file) {
        logger.info("Album upload request: albumId={}, originalFilename={}, size={}",
                albumId, file.getOriginalFilename(), file.getSize());
        return storageService.uploadForAlbum(albumId, file);
    }

    @GetMapping("/files")
    public List<StoredFile> listFiles() {
        return storageService.listAll();
    }

    @GetMapping("/albums/{albumId}/files")
    public List<StoredFile> listAlbumFiles(@PathVariable String albumId) {
        return storageService.listByAlbum(albumId);
    }

    /**
     * Proxies file download from ECS through the server.
     * Forces Content-Disposition: attachment to prevent browser from rendering potentially
     * dangerous content (e.g. SVG with scripts, HTML files).
     */
    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam("key") String key) {
        logger.info("Download request for key: {}", key);

        String contentType = storageService.getContentType(key);
        if (!StringUtils.hasText(contentType)) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        String filename = extractFilename(key);
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = storageService.download(key)) {
                byte[] buffer = new byte[STREAM_BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                .header(HttpHeaders.X_CONTENT_TYPE_OPTIONS, "nosniff")
                .body(body);
    }

    @DeleteMapping("/files")
    public ResponseEntity<Void> deleteFile(@RequestParam("key") String key) {
        logger.info("Delete request for key: {}", key);
        storageService.delete(key);
        return ResponseEntity.noContent().build();
    }

    private String extractFilename(String key) {
        int lastSlash = key.lastIndexOf('/');
        String withUuid = lastSlash >= 0 ? key.substring(lastSlash + 1) : key;
        if (withUuid.length() > 37 && withUuid.charAt(36) == '-') {
            return withUuid.substring(37);
        }
        return withUuid;
    }
}
