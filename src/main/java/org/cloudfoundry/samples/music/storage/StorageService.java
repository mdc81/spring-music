package org.cloudfoundry.samples.music.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface StorageService {

    /**
     * Stores a file under the general uploads prefix and returns its metadata.
     */
    StoredFile upload(MultipartFile file);

    /**
     * Stores a file scoped to a specific album and returns its metadata.
     */
    StoredFile uploadForAlbum(String albumId, MultipartFile file);

    /**
     * Lists all files in the bucket (general uploads only).
     */
    List<StoredFile> listAll();

    /**
     * Lists all files associated with a specific album.
     */
    List<StoredFile> listByAlbum(String albumId);

    /**
     * Opens a stream to the object identified by key. Caller is responsible for closing.
     */
    InputStream download(String key);

    /**
     * Returns the content-type stored alongside the object.
     */
    String getContentType(String key);

    /**
     * Deletes the object identified by key.
     */
    void delete(String key);
}
