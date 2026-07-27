package com.blog.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ImageValidationPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @ParameterizedTest
    @ValueSource(strings = {
            "upload.image.max-dimension=0",
            "upload.image.max-frame-pixels=50000001"
    })
    void invalidImageLimitConfigurationFailsStartup(String invalidProperty) {
        contextRunner
                .withPropertyValues(
                        "upload.image.max-total-pixels=50000000",
                        invalidProperty)
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ImageValidationProperties.class)
    static class TestConfiguration {
    }
}
