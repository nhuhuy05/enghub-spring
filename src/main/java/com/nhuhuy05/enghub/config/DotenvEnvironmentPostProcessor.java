package com.nhuhuy05.enghub.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String PROPERTY_SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        Map<String, Object> properties = new LinkedHashMap<>();
        dotenv.entries().forEach(entry -> properties.put(entry.getKey(), entry.getValue()));

        addRenderJdbcUrl(environment, properties);

        if (!properties.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
    }

    private void addRenderJdbcUrl(ConfigurableEnvironment environment, Map<String, Object> properties) {
        if (environment.getProperty("SPRING_DATASOURCE_URL") != null || properties.containsKey("SPRING_DATASOURCE_URL")) {
            return;
        }

        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null) {
            Object dotenvDatabaseUrl = properties.get("DATABASE_URL");
            databaseUrl = dotenvDatabaseUrl != null ? dotenvDatabaseUrl.toString() : null;
        }

        if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
            properties.put("SPRING_DATASOURCE_URL", "jdbc:" + databaseUrl);
            addRenderDatabaseCredentials(environment, properties, databaseUrl);
        }
    }

    private void addRenderDatabaseCredentials(
            ConfigurableEnvironment environment, Map<String, Object> properties, String databaseUrl) {
        if (environment.getProperty("SPRING_DATASOURCE_USERNAME") != null
                || environment.getProperty("SPRING_DATASOURCE_PASSWORD") != null
                || properties.containsKey("SPRING_DATASOURCE_USERNAME")
                || properties.containsKey("SPRING_DATASOURCE_PASSWORD")) {
            return;
        }

        URI uri = URI.create(databaseUrl);
        String userInfo = uri.getUserInfo();
        if (userInfo == null || userInfo.isBlank()) {
            return;
        }

        String[] credentialParts = userInfo.split(":", 2);
        properties.put("SPRING_DATASOURCE_USERNAME", decodeUrlPart(credentialParts[0]));
        if (credentialParts.length > 1) {
            properties.put("SPRING_DATASOURCE_PASSWORD", decodeUrlPart(credentialParts[1]));
        }
    }

    private String decodeUrlPart(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
