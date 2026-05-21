package com.nhuhuy05.enghub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {
    String cloudName;
    String apiKey;
    String apiSecret;
    String folderRoot = "enghub";
}
