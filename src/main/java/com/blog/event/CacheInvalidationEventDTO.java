package com.blog.event;

import java.io.Serializable;

public class CacheInvalidationEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cacheKey;
    private CacheOperation operation;
    private Object value;
    private long executeTime;

    public CacheInvalidationEventDTO() {
    }

    public CacheInvalidationEventDTO(String cacheKey, CacheOperation operation, Object value, long executeTime) {
        this.cacheKey = cacheKey;
        this.operation = operation;
        this.value = value;
        this.executeTime = executeTime;
    }

    public static CacheInvalidationEventDTO fromEvent(CacheInvalidationEvent event) {
        return new CacheInvalidationEventDTO(
            event.getCacheKey(),
            event.getOperation(),
            event.getValue(),
            event.getExecuteTime()
        );
    }

    public CacheInvalidationEvent toEvent(Object source) {
        long delayMs = Math.max(0, executeTime - System.currentTimeMillis());
        return new CacheInvalidationEvent(source, cacheKey, operation, value, delayMs);
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public CacheOperation getOperation() {
        return operation;
    }

    public void setOperation(CacheOperation operation) {
        this.operation = operation;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(long executeTime) {
        this.executeTime = executeTime;
    }

    public long getRemainingDelayMs() {
        return Math.max(0, executeTime - System.currentTimeMillis());
    }

    public boolean isReadyToExecute() {
        return System.currentTimeMillis() >= executeTime;
    }

    @Override
    public String toString() {
        return "CacheInvalidationEventDTO{" +
                "cacheKey='" + cacheKey + '\'' +
                ", operation=" + operation +
                ", executeTime=" + executeTime +
                '}';
    }
}
