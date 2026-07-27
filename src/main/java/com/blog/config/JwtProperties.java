package com.blog.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank
    @Size(min = 32)
    private String secret;

    @NotBlank
    @Size(min = 32)
    private String refreshSecret;

    @Min(60)
    @Max(3600)
    private long accessExpirationSeconds = 900;

    @Min(86400)
    @Max(2592000)
    private long refreshExpirationSeconds = 604800;
}
