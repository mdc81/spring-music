package org.cloudfoundry.samples.music.domain;

import org.junit.Test;
import static org.junit.Assert.*;

public class AlbumTest {

    @Test
    public void testParameterizedConstructor() {
        Album album = new Album("Test Title", "Test Artist", "2024", "Pop");
        assertEquals("Test Title", album.getTitle());
        assertEquals("Test Artist", album.getArtist());
        assertEquals("2024", album.getReleaseYear());
        assertEquals("Pop", album.getGenre());
    }

    @Test
    public void testEmptyConstructor() {
        Album album = new Album();
        assertNull(album.getId());
        assertNull(album.getTitle());
        assertNull(album.getArtist());
        assertNull(album.getReleaseYear());
        assertNull(album.getGenre());
        assertNull(album.getAlbumId());
        assertEquals(0, album.getTrackCount());
    }

    @Test
    public void testSetId() {
        Album album = new Album();
        album.setId("id123");
        assertEquals("id123", album.getId());
    }

    @Test
    public void testSetTitle() {
        Album album = new Album();
        album.setTitle("My Title");
        assertEquals("My Title", album.getTitle());
    }

    @Test
    public void testSetArtist() {
        Album album = new Album();
        album.setArtist("My Artist");
        assertEquals("My Artist", album.getArtist());
    }

    @Test
    public void testSetReleaseYear() {
        Album album = new Album();
        album.setReleaseYear("2025");
        assertEquals("2025", album.getReleaseYear());
    }

    @Test
    public void testSetGenre() {
        Album album = new Album();
        album.setGenre("Rock");
        assertEquals("Rock", album.getGenre());
    }

    @Test
    public void testSetTrackCount() {
        Album album = new Album();
        album.setTrackCount(12);
        assertEquals(12, album.getTrackCount());
    }

    @Test
    public void testSetAlbumId() {
        Album album = new Album();
        album.setAlbumId("album456");
        assertEquals("album456", album.getAlbumId());
    }
}
