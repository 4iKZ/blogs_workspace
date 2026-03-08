package com.blog.controller;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ContentRequest;
import com.blog.dto.SensitiveCheckResultDTO;
import com.blog.dto.SensitiveWordCreateDTO;
import com.blog.dto.SensitiveWordDTO;
import com.blog.service.SensitiveWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 后台敏感词管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sensitive-words")
@Tag(name = "后台敏感词管理接口")
public class SensitiveWordController {

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @GetMapping
    @Operation(summary = "获取敏感词列表")
    public Result<PageResult<SensitiveWordDTO>> getWordList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类筛选") @RequestParam(required = false) String category) {
        return sensitiveWordService.getWordList(page, size, keyword, category);
    }

    @PostMapping
    @Operation(summary = "新增敏感词")
    public Result<Long> addWord(@Valid @RequestBody SensitiveWordCreateDTO createDTO) {
        return sensitiveWordService.addWord(createDTO);
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑敏感词")
    public Result<Void> updateWord(
            @Parameter(description = "敏感词ID") @PathVariable Long id,
            @Valid @RequestBody SensitiveWordCreateDTO createDTO) {
        return sensitiveWordService.updateWord(id, createDTO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除敏感词")
    public Result<Void> deleteWord(@Parameter(description = "敏感词ID") @PathVariable Long id) {
        return sensitiveWordService.deleteWord(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除敏感词")
    public Result<Void> batchDeleteWords(@RequestBody List<Long> ids) {
        return sensitiveWordService.batchDeleteWords(ids);
    }

    @PostMapping("/batch-import")
    @Operation(summary = "批量导入敏感词")
    public Result<Integer> batchImport(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> words = (List<String>) body.get("words");
        String category = (String) body.getOrDefault("category", "default");
        Integer level = body.get("level") != null ? Integer.valueOf(body.get("level").toString()) : 1;

        return sensitiveWordService.batchImport(words, category, level);
    }

    @PostMapping("/reload-cache")
    @Operation(summary = "重载敏感词缓存")
    public Result<Void> reloadCache() {
        return sensitiveWordService.reloadCache();
    }

    @PostMapping("/check")
    @Operation(summary = "检测文本是否包含敏感词")
    public Result<SensitiveCheckResultDTO> checkContent(@RequestBody ContentRequest contentRequest) {
        return sensitiveWordService.checkContent(contentRequest.getContent());
    }
}
