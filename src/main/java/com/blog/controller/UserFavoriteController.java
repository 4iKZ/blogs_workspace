package com.blog.controller;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.UserFavoriteDTO;
import com.blog.service.UserFavoriteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户收藏控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user/favorite")
@Tag(name = "用户收藏接口")
public class UserFavoriteController {

    @Autowired
    private UserFavoriteService userFavoriteService;

    @PostMapping("/{articleId}")
    @Operation(summary = "收藏文章")
    public Result<Long> favoriteArticle(@Parameter(description = "文章ID") @PathVariable Long articleId) {
        return userFavoriteService.favoriteArticle(articleId);
    }

    @DeleteMapping("/{articleId}")
    @Operation(summary = "取消收藏文章")
    public Result<Void> unfavoriteArticle(@Parameter(description = "文章ID") @PathVariable Long articleId) {
        return userFavoriteService.unfavoriteArticle(articleId);
    }

    @GetMapping("/list")
    @Operation(summary = "获取用户收藏列表")
    public Result<PageResult<UserFavoriteDTO>> getUserFavorites(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return userFavoriteService.getUserFavorites(page, size);
    }

    @GetMapping("/{articleId}/check")
    @Operation(summary = "检查文章是否已收藏")
    public Result<Boolean> isArticleFavorited(@Parameter(description = "文章ID") @PathVariable Long articleId) {
        return userFavoriteService.isArticleFavorited(articleId);
    }

    @GetMapping("/count")
    @Operation(summary = "获取用户收藏数量")
    public Result<Integer> getUserFavoriteCount() {
        return userFavoriteService.getUserFavoriteCount();
    }
}
