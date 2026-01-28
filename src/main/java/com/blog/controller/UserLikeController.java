package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.UserLikeDTO;
import com.blog.service.UserLikeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.blog.common.PageResult;

/**
 * 用户点赞控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user/like")
@Tag(name = "用户点赞接口")
public class UserLikeController {

    @Autowired
    private UserLikeService userLikeService;

    @PostMapping("/{articleId}")
    @Operation(summary = "点赞文章")
    public Result<Long> likeArticle(@Parameter(description = "文章ID") @PathVariable Long articleId) {
        return userLikeService.likeArticle(articleId);
    }

    @DeleteMapping("/{articleId}")
    @Operation(summary = "取消点赞文章")
    public Result<Void> unlikeArticle(@Parameter(description = "文章ID") @PathVariable Long articleId) {
        return userLikeService.unlikeArticle(articleId);
    }

    @GetMapping("/list")
    @Operation(summary = "获取用户点赞列表")
    public Result<PageResult<UserLikeDTO>> getUserLikes(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return userLikeService.getUserLikes(page, size);
    }

    @GetMapping("/{articleId}/check")
    @Operation(summary = "检查文章是否已点赞")
    public Result<Boolean> isArticleLiked(@Parameter(description = "文章ID") @PathVariable Long articleId) {
        return userLikeService.isArticleLiked(articleId);
    }

    @GetMapping("/count")
    @Operation(summary = "获取用户点赞数量")
    public Result<Integer> getUserLikeCount() {
        return userLikeService.getUserLikeCount();
    }
}
