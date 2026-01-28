package com.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BlogBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlogBackendApplication.class, args);
	}

@org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner commandLineRunner(org.springframework.core.env.Environment env) {
        return args -> {
            System.out.println("========================================");
            System.out.println("ACTIVE REDIS CONFIGURATION:");
            System.out.println("Host: " + env.getProperty("spring.redis.host"));
            System.out.println("Port: " + env.getProperty("spring.redis.port"));
            System.out.println("Docker Compose Enabled: " + env.getProperty("spring.docker.compose.enabled"));
            System.out.println("========================================");
        };
    }
}
