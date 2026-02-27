package org.cloudfoundry.samples.music.repositories;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.cloudfoundry.samples.music.domain.Album;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public class AlbumRepositoryPopulator implements ApplicationListener<ApplicationReadyEvent> {
    private final ObjectMapper objectMapper;
    private final Resource sourceData;

    public AlbumRepositoryPopulator() {
        objectMapper = JsonMapper.builder().build();
        sourceData = new ClassPathResource("albums.json");
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        CrudRepository albumRepository =
                BeanFactoryUtils.beanOfTypeIncludingAncestors(event.getApplicationContext(), CrudRepository.class);

        if (albumRepository != null && albumRepository.count() == 0) {
            populate(albumRepository);
        }
    }

    @SuppressWarnings("unchecked")
    private void populate(CrudRepository repository) {
        try {
            List<Album> albums = objectMapper.readValue(
                    sourceData.getInputStream(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Album.class));
            for (Album album : albums) {
                if (album != null) {
                    repository.save(album);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
