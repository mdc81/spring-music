package org.cloudfoundry.samples.music.config.data;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.cloudfoundry.samples.music.domain.Album;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Profile("redis")
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Album> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Album> template = new RedisTemplate<>();

        template.setConnectionFactory(redisConnectionFactory);

        RedisSerializer<String> stringSerializer = new StringRedisSerializer();
        RedisSerializer<Album> albumSerializer = albumSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(albumSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(albumSerializer);

        return template;
    }

    private RedisSerializer<Album> albumSerializer() {
        ObjectMapper mapper = JsonMapper.builder().build();
        return new RedisSerializer<Album>() {
            @Override
            public byte[] serialize(Album album) throws SerializationException {
                try {
                    return mapper.writeValueAsBytes(album);
                } catch (Exception e) {
                    throw new SerializationException("Could not serialize Album", e);
                }
            }

            @Override
            public Album deserialize(byte[] bytes) throws SerializationException {
                if (bytes == null) return null;
                try {
                    return mapper.readValue(bytes, Album.class);
                } catch (Exception e) {
                    throw new SerializationException("Could not deserialize Album", e);
                }
            }
        };
    }

}
