package com.blog.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
    }
}
