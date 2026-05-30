package com.nhuhuy05.enghub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    String apiKey;
    String model = "gemini-3.1-flash-lite";
    boolean enabled = true;
    boolean deleteFileAfterUse = true;
}
