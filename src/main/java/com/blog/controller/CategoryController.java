package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.CategoryCreateDTO;
import com.blog.dto.CategoryDTO;
import com.blog.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 分类管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/category")
@Tag(name = "分类管理接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "获取分类列表")
    public Result<List<CategoryDTO>> getCategoryList() {
        return categoryService.getCategoryList();
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "获取分类详情")
    public Result<CategoryDTO> getCategoryById(@Parameter(description = "分类ID") @PathVariable Long categoryId) {
        return categoryService.getCategoryById(categoryId);
    }

    @PostMapping
    @Operation(summary = "添加分类")
    public Result<Long> addCategory(@Valid @RequestBody CategoryCreateDTO categoryCreateDTO) {
        return categoryService.addCategory(categoryCreateDTO);
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "编辑分类")
    public Result<Void> updateCategory(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Valid @RequestBody CategoryCreateDTO categoryCreateDTO) {
        return categoryService.updateCategory(categoryId, categoryCreateDTO);
    }

    @DeleteMapping("/{categoryId}")
    @Operation(summary = "删除分类")
    public Result<Void> deleteCategory(@Parameter(description = "分类ID") @PathVariable Long categoryId) {
        return categoryService.deleteCategory(categoryId);
    }

    @GetMapping("/{categoryId}/count")
    @Operation(summary = "获取分类下的文章数量")
    public Result<Integer> getCategoryArticleCount(@Parameter(description = "分类ID") @PathVariable Long categoryId) {
        return categoryService.getCategoryArticleCount(categoryId);
    }
}