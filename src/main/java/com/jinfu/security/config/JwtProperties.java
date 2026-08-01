package com.jinfu.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String secret;
    private long expire = 86400;
    private String header = "Authorization";
    private String tokenPrefix = "Bearer ";
}
