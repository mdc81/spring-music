package org.cloudfoundry.samples.music.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Profile("ecs")
public class EcsStorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(EcsStorageService.class);
    private static final String GENERAL_PREFIX = "uploads/";
    private static final String ALBUM_PREFIX = "albums/";

    private final S3Client s3Client;
    private final String bucketName;
    private final FileValidator fileValidator;

    public EcsStorageService(S3Client s3Client,
                             @Qualifier("ecsBucketName") String bucketName,
                             FileValidator fileValidator) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.fileValidator = fileValidator;
    }

    @Override
    public StoredFile upload(MultipartFile file) {
        fileValidator.validate(file);
        String key = GENERAL_PREFIX + UUID.randomUUID() + "-" + fileValidator.sanitizeFilename(file.getOriginalFilename());
        return putObject(key, file);
    }

    @Override
    public StoredFile uploadForAlbum(String albumId, MultipartFile file) {
        fileValidator.validate(file);
        validateAlbumId(albumId);
        String key = ALBUM_PREFIX + albumId + "/" + UUID.randomUUID() + "-" + fileValidator.sanitizeFilename(file.getOriginalFilename());
        return putObject(key, file);
    }

    @Override
    public List<StoredFile> listAll() {
        return listByPrefix(GENERAL_PREFIX);
    }

    @Override
    public List<StoredFile> listByAlbum(String albumId) {
        validateAlbumId(albumId);
        return listByPrefix(ALBUM_PREFIX + albumId + "/");
    }

    @Override
    public InputStream download(String key) {
        validateKey(key);
        try {
            return s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
    }

    @Override
    public String getContentType(String key) {
        validateKey(key);
        try {
            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return head.contentType();
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        logger.info("Deleting object: {}", key);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }

    private StoredFile putObject(String key, MultipartFile file) {
        try {
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(contentType)
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            logger.info("Stored object: {}", key);
            return new StoredFile(key, extractFilename(key), contentType, file.getSize(), Instant.now());
        } catch (IOException e) {
            logger.error("Failed to read upload stream for key {}: {}", key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process uploaded file");
        }
    }

    private List<StoredFile> listByPrefix(String prefix) {
        ListObjectsV2Response response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .build()
        );
        return response.contents().stream()
                .map(this::toStoredFile)
                .collect(Collectors.toList());
    }

    private StoredFile toStoredFile(S3Object s3Object) {
        return new StoredFile(
                s3Object.key(),
                extractFilename(s3Object.key()),
                null,
                s3Object.size(),
                s3Object.lastModified()
        );
    }

    private String extractFilename(String key) {
        int lastSlash = key.lastIndexOf('/');
        String withUuid = lastSlash >= 0 ? key.substring(lastSlash + 1) : key;
        // Strip leading UUID prefix (36 chars + dash)
        if (withUuid.length() > 37 && withUuid.charAt(36) == '-') {
            return withUuid.substring(37);
        }
        return withUuid;
    }

    /**
     * Ensures the key stays within the expected prefixes to prevent access to arbitrary bucket paths.
     */
    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file key");
        }
        if (!key.startsWith(GENERAL_PREFIX) && !key.startsWith(ALBUM_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access to this resource is not permitted");
        }
        if (key.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file key");
        }
    }

    /**
     * Album IDs are random hex strings from the existing RandomIdGenerator.
     * Reject anything that doesn't match to prevent path injection via the album ID segment.
     */
    private void validateAlbumId(String albumId) {
        if (albumId == null || !albumId.matches("[a-zA-Z0-9\\-]{1,64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid album identifier");
        }
    }
}
