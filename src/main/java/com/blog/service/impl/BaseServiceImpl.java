package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.PageDTO;
import com.blog.service.BaseService;
import com.blog.utils.BusinessUtils;
import com.blog.utils.PageUtils;

import java.util.List;

/**
 * 通用Service实现类
 * 实现BaseService接口中的通用方法
 * 所有Service实现类的父类
 * @param <T> 实体类型
 * @param <D> DTO类型
 * @param <C> 创建DTO类型
 * @param <U> 更新DTO类型
 */
public abstract class BaseServiceImpl<T, D, C, U> implements BaseService<T, D, C, U> {

    /**
     * 实体Mapper，由子类注入
     */
    protected abstract BaseMapper<T> getBaseMapper();

    @Override
    public D findById(Long id) {
        T entity = BusinessUtils.checkIdExist(id, getBaseMapper()::selectById, "对象不存在");
        return toDTO(entity);
    }

    @Override
    public Long save(C createDTO) {
        T entity = toEntity(createDTO);
        int result = getBaseMapper().insert(entity);
        if (result <= 0) {
            throw new RuntimeException("保存对象失败");
        }
        // 假设实体类有getId()方法获取主键
        try {
            return (Long) entity.getClass().getMethod("getId").invoke(entity);
        } catch (Exception e) {
            throw new RuntimeException("获取对象ID失败", e);
        }
    }

    @Override
    public void update(Long id, U updateDTO) {
        T entity = BusinessUtils.checkIdExist(id, getBaseMapper()::selectById, "对象不存在");
        T updatedEntity = updateEntity(entity, updateDTO);
        BusinessUtils.setUpdateTime((BusinessUtils.Updatable) updatedEntity);
        int result = getBaseMapper().updateById(updatedEntity);
        if (result <= 0) {
            throw new RuntimeException("更新对象失败");
        }
    }

    @Override
    public void delete(Long id) {
        BusinessUtils.checkIdExist(id, getBaseMapper()::selectById, "对象不存在");
        int result = getBaseMapper().deleteById(id);
        if (result <= 0) {
            throw new RuntimeException("删除对象失败");
        }
    }

    @Override
    public PageDTO<D> findPage(Integer page, Integer size) {
        Page<T> pageObj = PageUtils.createPage(page, size);
        IPage<T> pageResult = getBaseMapper().selectPage(pageObj, null);
        return PageUtils.convertPageResult(pageResult, this::toDTO);
    }

    @Override
    public List<D> findAll() {
        List<T> entities = getBaseMapper().selectList(null);
        return PageUtils.convertList(entities, this::toDTO);
    }

    @Override
    public List<D> findByIds(List<Long> ids) {
        List<T> entities = getBaseMapper().selectBatchIds(ids);
        return PageUtils.convertList(entities, this::toDTO);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        T entity = BusinessUtils.checkIdExist(id, getBaseMapper()::selectById, "对象不存在");
        // 假设实体类有setStatus()方法
        try {
            entity.getClass().getMethod("setStatus", Integer.class).invoke(entity, status);
            BusinessUtils.setUpdateTime((BusinessUtils.Updatable) entity);
            int result = getBaseMapper().updateById(entity);
            if (result <= 0) {
                throw new RuntimeException("更新状态失败");
            }
        } catch (Exception e) {
            throw new RuntimeException("更新状态失败", e);
        }
    }

    @Override
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        int result = getBaseMapper().deleteBatchIds(ids);
        if (result <= 0) {
            throw new RuntimeException("批量删除失败");
        }
    }

    @Override
    public Long count() {
        return getBaseMapper().selectCount(null);
    }

    @Override
    public boolean existsById(Long id) {
        return BusinessUtils.isValidId(id) && getBaseMapper().selectById(id) != null;
    }

    /**
     * 转换实体到DTO
     * 由子类实现具体转换逻辑
     * @param entity 实体对象
     * @return DTO对象
     */
    @Override
    public abstract D toDTO(T entity);

    /**
     * 转换DTO到实体
     * 由子类实现具体转换逻辑
     * @param createDTO 创建DTO
     * @return 实体对象
     */
    @Override
    public abstract T toEntity(C createDTO);

    /**
     * 更新实体
     * 由子类实现具体更新逻辑
     * @param entity 实体对象
     * @param updateDTO 更新DTO
     * @return 更新后的实体对象
     */
    @Override
    public abstract T updateEntity(T entity, U updateDTO);

    /**
     * 创建Lambda查询包装器
     * @return LambdaQueryWrapper对象
     */
    protected LambdaQueryWrapper<T> createQueryWrapper() {
        return new LambdaQueryWrapper<>();
    }
}
