package com.blog.service;

import com.blog.dto.PageDTO;

/**
 * 通用 Service 接口
 * 封装所有 Service 类共有的 CRUD 方法
 * @param <T> 实体类型
 * @param <D> DTO 类型
 * @param <C> 创建 DTO 类型
 * @param <U> 更新 DTO 类型
 */
public interface BaseService<T, D, C, U> {

    /**
     * 根据 ID 查找对象
     * @param id ID 值
     * @return 查找的对象 DTO
     */
    D findById(Long id);

    /**
     * 保存对象
     * @param createDTO 创建 DTO
     * @return 保存后的对象 ID
     */
    Long save(C createDTO);

    /**
     * 更新对象
     * @param id 对象 ID
     * @param updateDTO 更新 DTO
     */
    void update(Long id, U updateDTO);

    /**
     * 删除对象
     * @param id 对象 ID
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
     * 根据 ID 列表查询对象
     * @param ids ID 列表
     * @return 对象列表
     */
    List<D> findByIds(List<Long> ids);

    /**
     * 更新对象状态
     * @param id 对象 ID
     * @param status 新状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 统计对象数量
     * @return 对象数量
     */
    Long count();

    /**
     * 检查对象是否存在
     * @param id 对象 ID
     * @return 是否存在
     */
    boolean existsById(Long id);

    /**
     * 转换实体到 DTO
     * @param entity 实体对象
     * @return DTO 对象
     */
    D toDTO(T entity);

    /**
     * 转换 DTO 到实体
     * @param createDTO 创建 DTO
     * @return 实体对象
     */
    T toEntity(C createDTO);

    /**
     * 更新实体
     * @param entity 实体对象
     * @param updateDTO 更新 DTO
     * @return 更新后的实体对象
     */
    T updateEntity(T entity, U updateDTO);
}
