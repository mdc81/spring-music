package org.cloudfoundry.samples.music.config.data;

import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Creates an S3Client configured for DellEMC ECS when an ECS service is bound via CF.
 * Credentials are sourced from VCAP_SERVICES via CfEnv and never exposed to clients.
 * Path-style access is enabled because ECS does not support virtual-hosted-style by default.
 */
@Configuration
@Profile("ecs")
public class EcsStorageConfig {

    private static final Log logger = LogFactory.getLog(EcsStorageConfig.class);

    private static final List<String> ECS_TAGS = Arrays.asList("ecs", "s3", "objectstore", "object-store");

    private final EcsCredentials credentials = resolveCredentials(new CfEnv());

    @Bean
    public S3Client ecsS3Client() {
        logger.info("Configuring ECS S3 client for endpoint: " + credentials.endpoint);

        return S3Client.builder()
                .endpointOverride(URI.create(credentials.endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(credentials.accessKey, credentials.secretKey)))
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean("ecsBucketName")
    public String ecsBucketName() {
        return credentials.bucket;
    }

    private EcsCredentials resolveCredentials(CfEnv cfEnv) {
        for (CfService service : cfEnv.findAllServices()) {
            Map<String, Object> creds = service.getCredentials().getMap();

            boolean tagMatch = service.getTags().stream()
                    .anyMatch(tag -> ECS_TAGS.stream().anyMatch(t -> t.equalsIgnoreCase(tag)));
            boolean credMatch = (creds.containsKey("endpoint") || creds.containsKey("s3Endpoint"))
                    && (creds.containsKey("accessKey") || creds.containsKey("access_key_id"));

            if (tagMatch || credMatch) {
                String endpoint = getString(creds, "endpoint", "s3Endpoint");
                String accessKey = getString(creds, "accessKey", "access_key_id");
                String secretKey = getString(creds, "secretKey", "secret_access_key");
                String bucket = getString(creds, "bucket", "bucketName", "bucket_name");

                if (endpoint == null || accessKey == null || secretKey == null || bucket == null) {
                    throw new IllegalStateException(
                            "ECS service binding is missing required credential fields " +
                            "(endpoint, accessKey/access_key_id, secretKey/secret_access_key, bucket). " +
                            "Check the service broker configuration.");
                }
                return new EcsCredentials(endpoint, accessKey, secretKey, bucket);
            }
        }
        throw new IllegalStateException(
                "ECS profile is active but no compatible service binding was found in VCAP_SERVICES. " +
                "Bind an ECS bucket service and restage the application.");
    }

    private String getString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }

    private static class EcsCredentials {
        final String endpoint;
        final String accessKey;
        final String secretKey;
        final String bucket;

        EcsCredentials(String endpoint, String accessKey, String secretKey, String bucket) {
            this.endpoint = endpoint;
            this.accessKey = accessKey;
            this.secretKey = secretKey;
            this.bucket = bucket;
        }
    }
}
