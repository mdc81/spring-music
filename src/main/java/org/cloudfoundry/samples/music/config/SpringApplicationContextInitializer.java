package org.cloudfoundry.samples.music.config;

import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpringApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger logger = LoggerFactory.getLogger(SpringApplicationContextInitializer.class);

    private static final Map<String, List<String>> profileNameToServiceTags = Map.of(
            "mongodb",   List.of("mongodb"),
            "postgres",  List.of("postgres"),
            "mysql",     List.of("mysql"),
            "redis",     List.of("redis"),
            "oracle",    List.of("oracle"),
            "sqlserver", List.of("sqlserver")
    );

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment appEnvironment = applicationContext.getEnvironment();

        validateActiveProfiles(appEnvironment);

        addCloudProfile(appEnvironment);

        excludeAutoConfiguration(appEnvironment);
    }

    private void addCloudProfile(ConfigurableEnvironment appEnvironment) {
        CfEnv cfEnv = new CfEnv();

        List<String> profiles = new ArrayList<>();

        List<CfService> services = cfEnv.findAllServices();
        List<String> serviceNames = services.stream()
                .map(CfService::getName)
                .collect(Collectors.toList());

        logger.info("Found services " + StringUtils.collectionToCommaDelimitedString(serviceNames));

        for (CfService service : services) {
            for (String profileKey : profileNameToServiceTags.keySet()) {
                if (service.getTags().containsAll(profileNameToServiceTags.get(profileKey))) {
                    profiles.add(profileKey);
                }
            }
        }

        if (profiles.size() > 1) {
            throw new IllegalStateException(
                    "Only one service of the following types may be bound to this application: " +
                            profileNameToServiceTags.values().toString() + ". " +
                            "These services are bound to the application: [" +
                            StringUtils.collectionToCommaDelimitedString(profiles) + "]");
        }

        if (profiles.size() > 0) {
            logger.info("Setting service profile " + profiles.get(0));
            appEnvironment.addActiveProfile(profiles.get(0));
        }
    }

    private void validateActiveProfiles(ConfigurableEnvironment appEnvironment) {
        Set<String> validLocalProfiles = profileNameToServiceTags.keySet();

        List<String> serviceProfiles = Stream.of(appEnvironment.getActiveProfiles())
                .filter(validLocalProfiles::contains)
                .collect(Collectors.toList());

        if (serviceProfiles.size() > 1) {
            throw new IllegalStateException("Only one active Spring profile may be set among the following: " +
                    validLocalProfiles.toString() + ". " +
                    "These profiles are active: [" +
                    StringUtils.collectionToCommaDelimitedString(serviceProfiles) + "]");
        }
    }

    private void excludeAutoConfiguration(ConfigurableEnvironment environment) {
        List<String> exclude = new ArrayList<>();
        if (environment.acceptsProfiles(Profiles.of("redis"))) {
            excludeDataSourceAutoConfiguration(exclude);
            excludeMongoAutoConfiguration(exclude);
        } else if (environment.acceptsProfiles(Profiles.of("mongodb"))) {
            excludeDataSourceAutoConfiguration(exclude);
            excludeRedisAutoConfiguration(exclude);
        } else {
            excludeMongoAutoConfiguration(exclude);
            excludeRedisAutoConfiguration(exclude);
        }

        Map<String, Object> properties = Map.of("spring.autoconfigure.exclude",
                StringUtils.collectionToCommaDelimitedString(exclude));

        PropertySource<?> propertySource = new MapPropertySource("springMusicAutoConfig", properties);

        environment.getPropertySources().addFirst(propertySource);
    }

    private void excludeDataSourceAutoConfiguration(List<String> exclude) {
        exclude.add(DataSourceAutoConfiguration.class.getName());
    }

    private void excludeMongoAutoConfiguration(List<String> exclude) {
        exclude.addAll(Arrays.asList(
                MongoAutoConfiguration.class.getName(),
                DataMongoAutoConfiguration.class.getName(),
                DataMongoRepositoriesAutoConfiguration.class.getName()
        ));
    }

    private void excludeRedisAutoConfiguration(List<String> exclude) {
        exclude.addAll(Arrays.asList(
                DataRedisAutoConfiguration.class.getName(),
                DataRedisRepositoriesAutoConfiguration.class.getName()
        ));
    }
}
