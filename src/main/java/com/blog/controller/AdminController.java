package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.blog.dto.CommentDTO;
import com.blog.dto.UserDTO;
import com.blog.service.AdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 后台管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@Tag(name = "后台管理接口")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // 用户管理
    @GetMapping("/users")
    @Operation(summary = "获取用户列表")
    public Result<List<UserDTO>> getUserList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "用户状态") @RequestParam(required = false) Integer status) {
        return adminService.getUserList(page, size, keyword, status);
    }

    @PutMapping("/users/{userId}/status")
    @Operation(summary = "修改用户状态")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "用户状态：0-禁用，1-正常") @RequestParam Integer status) {
        return adminService.updateUserStatus(userId, status);
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "删除用户")
    public Result<Void> deleteUser(@Parameter(description = "用户ID") @PathVariable Long userId) {
        return adminService.deleteUser(userId);
    }

    // 文章管理
    @GetMapping("/articles")
    @Operation(summary = "获取文章列表")
    public Result<List<ArticleDTO>> getArticleList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "文章状态") @RequestParam(required = false) Integer status) {
        return adminService.getArticleList(page, size, keyword, status);
    }

    @PutMapping("/articles/{articleId}/status")
    @Operation(summary = "修改文章状态")
    public Result<Void> updateArticleStatus(
            @Parameter(description = "文章ID") @PathVariable Long articleId,
            @Parameter(description = "文章状态：1-草稿，2-已发布，3-已下线") @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        return adminService.updateArticleStatus(articleId, status);
    }

    @DeleteMapping("/articles/{articleId}")
    @Operation(summary = "删除文章")
    public Result<Void> deleteArticle(@Parameter(description = "文章ID") @PathVariable Long articleId) {
        return adminService.deleteArticle(articleId);
    }

    // 评论管理
    @GetMapping("/comments")
    @Operation(summary = "获取评论列表")
    public Result<List<CommentDTO>> getCommentList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "评论状态：1-待审核，2-已通过，3-已拒绝，4-已删除") @RequestParam(required = false) Integer status,
            @Parameter(description = "文章ID") @RequestParam(required = false) Long articleId) {
        return adminService.getCommentList(page, size, keyword, status, articleId);
    }

    // 统计信息
    @GetMapping("/statistics")
    @Operation(summary = "获取网站统计信息")
    public Result<Map<String, Object>> getWebsiteStatistics() {
        return adminService.getWebsiteStatistics();
    }

    @GetMapping("/visit-statistics")
    @Operation(summary = "获取访问统计信息")
    public Result<Map<String, Object>> getVisitStatistics(
            @Parameter(description = "统计类型：day-日统计，week-周统计，month-月统计") @RequestParam String type) {
        // 根据类型生成对应的日期范围
        String startDate;
        String endDate;
        
        switch (type) {
            case "day":
                startDate = LocalDate.now().toString();
                endDate = LocalDate.now().toString();
                break;
            case "week":
                startDate = LocalDate.now().minusDays(7).toString();
                endDate = LocalDate.now().toString();
                break;
            case "month":
                startDate = LocalDate.now().minusDays(30).toString();
                endDate = LocalDate.now().toString();
                break;
            default:
                startDate = LocalDate.now().toString();
                endDate = LocalDate.now().toString();
        }
        
        return adminService.getVisitStatistics(startDate, endDate);
    }

    // 系统配置
    @GetMapping("/config")
    @Operation(summary = "获取系统配置")
    public Result<Map<String, String>> getSystemConfig() {
        return adminService.getSystemConfig();
    }

    @PutMapping("/config")
    @Operation(summary = "更新系统配置")
    public Result<Void> updateSystemConfig(@RequestBody Map<String, String> config) {
        return adminService.updateSystemConfig(config);
    }

    // 数据备份
    @PostMapping("/backup")
    @Operation(summary = "数据备份")
    public Result<String> backupDatabase() {
        return adminService.backupDatabase();
    }

    @PostMapping("/cache/clear")
    @Operation(summary = "清理缓存")
    public Result<Void> clearCache() {
        return adminService.clearCache();
    }
}