package org.cloudfoundry.samples.music.web;

import org.cloudfoundry.samples.music.domain.Album;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.repository.CrudRepository;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AlbumControllerTest {

    @Mock
    private CrudRepository<Album, String> repository;

    @InjectMocks
    private AlbumController albumController;

    @Test
    public void testAlbums() {
        Album album1 = new Album("Title1", "Artist1", "2020", "Pop");
        Album album2 = new Album("Title2", "Artist2", "2021", "Rock");
        when(repository.findAll()).thenReturn(Arrays.asList(album1, album2));

        Iterable<Album> albums = albumController.albums();
        assertNotNull(albums);
        int count = 0;
        for (Album a : albums) count++;
        assertEquals(2, count);
    }

    @Test
    public void testAdd() {
        Album album = new Album("Title", "Artist", "2020", "Pop");
        when(repository.save(album)).thenReturn(album);
        
        Album added = albumController.add(album);
        assertNotNull(added);
        assertEquals("Title", added.getTitle());
    }

    @Test
    public void testUpdate() {
        Album album = new Album("Title", "Artist", "2020", "Pop");
        when(repository.save(album)).thenReturn(album);

        Album updated = albumController.update(album);
        assertNotNull(updated);
        assertEquals("Title", updated.getTitle());
    }

    @Test
    public void testGetById() {
        Album album = new Album("Title", "Artist", "2020", "Pop");
        when(repository.findById("1")).thenReturn(Optional.of(album));

        Album fetched = albumController.getById("1");
        assertNotNull(fetched);
        assertEquals("Title", fetched.getTitle());
    }

    @Test
    public void testDeleteById() {
        albumController.deleteById("1");
        verify(repository, times(1)).deleteById("1");
    }
}
