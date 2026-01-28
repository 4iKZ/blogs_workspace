package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.CommentCreateDTO;
import com.blog.dto.CommentDTO;
import com.blog.dto.ContentRequest;
import com.blog.service.CommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 评论管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/comment")
@Tag(name = "评论管理接口")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HttpServletRequest request;

    @PostMapping
    @Operation(summary = "发表评论")
    public Result<Long> createComment(@Valid @RequestBody CommentCreateDTO commentCreateDTO) {
        Long currentUserId = getCurrentUserId();
        commentCreateDTO.setUserId(currentUserId);
        return commentService.createComment(commentCreateDTO);
    }

    @GetMapping("/list")
    @Operation(summary = "获取评论列表")
    public Result<List<CommentDTO>> getCommentList(
            @Parameter(description = "文章ID") @RequestParam Long articleId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "评论状态：1-待审核，2-已通过，3-已拒绝，4-已删除") @RequestParam(required = false) Integer status,
            @Parameter(description = "排序方式：time-按时间，hot-按热度") @RequestParam(required = false, defaultValue = "time") String sortBy) {
        // 获取当前用户ID（如果已登录）
        Long currentUserId = null;
        try {
            currentUserId = getCurrentUserId();
        } catch (Exception e) {
            // 用户未登录，不影响评论列表获取
        }
        return commentService.getCommentList(articleId, page, size, status, sortBy, currentUserId);
    }

    @GetMapping("/{commentId}/like-status")
    @Operation(summary = "检查评论点赞状态")
    public Result<Boolean> checkCommentLikeStatus(
            @Parameter(description = "评论ID") @PathVariable Long commentId) {
        Long currentUserId = getCurrentUserId();
        return commentService.checkCommentLikeStatus(commentId, currentUserId);
    }

    @PostMapping("/like-status/batch")
    @Operation(summary = "批量检查评论点赞状态")
    public Result<java.util.Map<Long, Boolean>> batchCheckCommentLikeStatus(
            @Parameter(description = "评论ID列表") @RequestBody List<Long> commentIds) {
        Long currentUserId = null;
        try {
            currentUserId = getCurrentUserId();
        } catch (Exception e) {
            // 用户未登录，返回空结果
            return commentService.batchCheckCommentLikeStatus(commentIds, null);
        }
        return commentService.batchCheckCommentLikeStatus(commentIds, currentUserId);
    }

    @GetMapping("/hot")
    @Operation(summary = "获取热门评论")
    public Result<List<CommentDTO>> getHotComments(
            @Parameter(description = "文章ID") @RequestParam Long articleId,
            @Parameter(description = "获取数量") @RequestParam(defaultValue = "5") Integer limit) {
        return commentService.getHotComments(articleId, limit);
    }

    @PostMapping("/check-sensitive")
    @Operation(summary = "检测评论内容是否包含敏感词")
    public Result<Boolean> checkSensitiveWords(@RequestBody ContentRequest contentRequest) {
        return commentService.checkSensitiveWords(contentRequest.getContent());
    }

    @PostMapping("/replace-sensitive")
    @Operation(summary = "替换评论内容中的敏感词")
    public Result<String> replaceSensitiveWords(@RequestBody ContentRequest contentRequest) {
        return commentService.replaceSensitiveWords(contentRequest.getContent());
    }

    @GetMapping("/children")
    @Operation(summary = "获取评论的子评论列表")
    public Result<List<CommentDTO>> getChildComments(
            @Parameter(description = "父评论ID") @RequestParam Long parentId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return commentService.getChildComments(parentId, page, size);
    }

    @GetMapping("/{commentId}")
    @Operation(summary = "获取评论详情")
    public Result<CommentDTO> getCommentById(@Parameter(description = "评论ID") @PathVariable Long commentId) {
        return commentService.getCommentById(commentId);
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论")
    public Result<Void> deleteComment(@Parameter(description = "评论ID") @PathVariable Long commentId) {
        return commentService.deleteComment(commentId);
    }

    @PutMapping("/{commentId}/review")
    @Operation(summary = "审核评论")
    public Result<Void> reviewComment(
            @Parameter(description = "评论ID") @PathVariable Long commentId,
            @Parameter(description = "评论状态：0-待审核，1-已通过，2-已拒绝") @RequestParam Integer status) {
        return commentService.reviewComment(commentId, status);
    }

    @GetMapping("/article/{articleId}/count")
    @Operation(summary = "获取文章的评论数量")
    public Result<Integer> getArticleCommentCount(@Parameter(description = "文章ID") @PathVariable Long articleId) {
        return commentService.getArticleCommentCount(articleId);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户的评论列表")
    public Result<List<CommentDTO>> getUserComments(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return commentService.getUserComments(userId, page, size);
    }

    @PostMapping("/{commentId}/like")
    @Operation(summary = "评论点赞")
    public Result<Void> likeComment(@Parameter(description = "评论ID") @PathVariable Long commentId) {
        return commentService.likeComment(commentId);
    }

    @DeleteMapping("/{commentId}/like")
    @Operation(summary = "取消评论点赞")
    public Result<Void> unlikeComment(@Parameter(description = "评论ID") @PathVariable Long commentId) {
        return commentService.unlikeComment(commentId);
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    private Long getCurrentUserId() {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new com.blog.exception.BusinessException(com.blog.common.ResultCode.UNAUTHORIZED, "用户未登录");
        }
        return Long.valueOf(userId.toString());
    }
}
