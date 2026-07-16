package com.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ConfigCheckTest {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Test
    public void printConfig() {
        System.out.println("============================================");
        System.out.println("LOADED REDIS HOST: " + redisHost);
        System.out.println("============================================");
    }
}
