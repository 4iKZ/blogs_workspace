package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "分页数据")
public class PageDTO<T> {
    
    @Schema(description = "当前页码")
    private int current;
    
    @Schema(description = "每页大小")
    private int size;
    
    @Schema(description = "总记录数")
    private long total;
    
    @Schema(description = "总页数")
    private int pages;
    
    @Schema(description = "数据列表")
    private List<T> records;
    
    public PageDTO() {
    }
    
    public PageDTO(int current, int size, long total, List<T> records) {
        this.current = current;
        this.size = size;
        this.total = total;
        this.records = records;
        this.pages = (int) Math.ceil((double) total / size);
    }
    
    // Getter and Setter methods
    public int getCurrent() {
        return current;
    }
    
    public void setCurrent(int current) {
        this.current = current;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    
    public long getTotal() {
        return total;
    }
    
    public void setTotal(long total) {
        this.total = total;
    }
    
    public int getPages() {
        return pages;
    }
    
    public void setPages(int pages) {
        this.pages = pages;
    }
    
    public List<T> getRecords() {
        return records;
    }
    
    public void setRecords(List<T> records) {
        this.records = records;
    }
}