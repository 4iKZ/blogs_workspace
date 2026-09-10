package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.ArticleCreateDTO;
import com.blog.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ArticleServiceImplUnitTest {

    @InjectMocks
    private ArticleServiceImpl articleService;

    @Mock
    private UserService userService;

    @Test
    public void testPublishArticle_InvalidAuthor_ShouldFail() {
        // Arrange
        ArticleCreateDTO createDTO = new ArticleCreateDTO();
        createDTO.setTitle("Test Title");
        Long invalidAuthorId = 99999L;

        // Mock userService to return null for this ID
        when(userService.getUserById(invalidAuthorId)).thenReturn(null);

        // Act
        Result<Long> result = articleService.publishArticle(createDTO, invalidAuthorId);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("作者不存在", result.getMessage());
    }
}
