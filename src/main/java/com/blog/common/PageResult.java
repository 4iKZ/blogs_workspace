package com.blog.common;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结果类
 */
@Schema(description = "分页响应结果")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "数据列表")
    @JsonProperty("items")
    private List<T> items;

    @Schema(description = "总记录数")
    @JsonProperty("total")
    private Long total;

    @Schema(description = "当前页码")
    @JsonProperty("page")
    private Integer page;

    @Schema(description = "每页数量")
    @JsonProperty("size")
    private Integer size;

    public PageResult() {
    }

    public PageResult(List<T> items, Long total, Integer page, Integer size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResult<T> of(List<T> items, Long total, Integer page, Integer size) {
        return new PageResult<>(items, total, page, size);
    }

    public static <T> PageResult<T> empty(Integer page, Integer size) {
        return new PageResult<>(List.of(), 0L, page, size);
    }

    // Getters and Setters
    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    /**
     * 计算总页数
     */
    public Integer getTotalPages() {
        if (total == null || total == 0 || size == null || size == 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / size);
    }

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return page != null && size != null && total != null && page * size < total;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return page != null && page > 1;
    }

    @Override
    public String toString() {
        return "PageResult{" +
                "items=" + (items != null ? items.size() : 0) + " items" +
                ", total=" + total +
                ", page=" + page +
                ", size=" + size +
                '}';
    }
}
