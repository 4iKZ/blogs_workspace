package com.blog.service;

import com.blog.dto.PageDTO;

import java.util.List;

/**
 * 通用Service接口
 * 封装所有Service类共有的CRUD方法
 * @param <T> 实体类型
 * @param <D> DTO类型
 * @param <C> 创建DTO类型
 * @param <U> 更新DTO类型
 */
public interface BaseService<T, D, C, U> {

    /**
     * 根据ID查找对象
     * @param id ID值
     * @return 查找的对象DTO
     */
    D findById(Long id);

    /**
     * 保存对象
     * @param createDTO 创建DTO
     * @return 保存后的对象ID
     */
    Long save(C createDTO);

    /**
     * 更新对象
     * @param id 对象ID
     * @param updateDTO 更新DTO
     */
    void update(Long id, U updateDTO);

    /**
     * 删除对象
     * @param id 对象ID
     */
    void delete(Long id);

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    PageDTO<D> findPage(Integer page, Integer size);

    /**
     * 查询所有对象
     * @return 对象列表
     */
    List<D> findAll();

    /**
     * 根据ID列表查询对象
     * @param ids ID列表
     * @return 对象列表
     */
    List<D> findByIds(List<Long> ids);

    /**
     * 更新对象状态
     * @param id 对象ID
     * @param status 新状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 批量删除对象
     * @param ids ID列表
     */
    void batchDelete(List<Long> ids);

    /**
     * 统计对象数量
     * @return 对象数量
     */
    Long count();

    /**
     * 检查对象是否存在
     * @param id 对象ID
     * @return 是否存在
     */
    boolean existsById(Long id);

    /**
     * 转换实体到DTO
     * @param entity 实体对象
     * @return DTO对象
     */
    D toDTO(T entity);

    /**
     * 转换DTO到实体
     * @param createDTO 创建DTO
     * @return 实体对象
     */
    T toEntity(C createDTO);

    /**
     * 更新实体
     * @param entity 实体对象
     * @param updateDTO 更新DTO
     * @return 更新后的实体对象
     */
    T updateEntity(T entity, U updateDTO);
}
