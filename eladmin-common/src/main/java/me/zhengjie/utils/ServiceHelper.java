/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.zhengjie.utils;

import me.zhengjie.base.BaseMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.function.Function;

/**
 * Service层通用工具类
 * 提供常见的Service操作方法，减少重复代码
 * @author Zheng Jie
 * @date 2019年10月24日20:46:32
 */
public final class ServiceHelper {

    private ServiceHelper() {
    }

    /**
     * 根据ID查询实体并转换为DTO
     * @param repository Repository
     * @param mapper Mapper
     * @param id ID
     * @param entityName 实体名称
     * @param constructor 实体构造函数
     * @return DTO
     */
    public static <D, E, ID> D findById(JpaRepository<E, ID> repository, BaseMapper<D, E> mapper, 
                                        ID id, String entityName, java.util.function.Supplier<E> constructor) {
        E entity = repository.findById(id).orElseGet(constructor);
        ValidationUtil.isNull(getEntityId(entity), entityName, "id", id);
        return mapper.toDto(entity);
    }

    /**
     * 根据ID查询原始实体（不转换为DTO）
     * @param repository Repository
     * @param id ID
     * @param entityName 实体名称
     * @param constructor 实体构造函数
     * @return 实体
     */
    public static <E, ID> E findByIdRaw(JpaRepository<E, ID> repository, 
                                        ID id, String entityName, java.util.function.Supplier<E> constructor) {
        E entity = repository.findById(id).orElseGet(constructor);
        ValidationUtil.isNull(getEntityId(entity), entityName, "id", id);
        return entity;
    }

    /**
     * 根据ID查询实体并转换为DTO（带缓存）
     * @param repository Repository
     * @param mapper Mapper
     * @param id ID
     * @param entityName 实体名称
     * @param constructor 实体构造函数
     * @param redisUtils Redis工具
     * @param cacheKey 缓存Key
     * @param entityClass 实体类
     * @return DTO
     */
    public static <D, E, ID> D findByIdWithCache(JpaRepository<E, ID> repository, BaseMapper<D, E> mapper,
                                                  ID id, String entityName, java.util.function.Supplier<E> constructor,
                                                  RedisUtils redisUtils, String cacheKey, Class<E> entityClass) {
        E entity = redisUtils.get(cacheKey, entityClass);
        if (entity == null) {
            entity = repository.findById(id).orElseGet(constructor);
            ValidationUtil.isNull(getEntityId(entity), entityName, "id", id);
            redisUtils.set(cacheKey, entity, 1, java.util.concurrent.TimeUnit.DAYS);
        }
        return mapper.toDto(entity);
    }

    /**
     * 分页查询并转换为分页结果
     * @param page 分页数据
     * @param mapper Mapper
     * @return 分页结果
     */
    public static <D, E> PageResult<D> toPageResult(Page<E> page, BaseMapper<D, E> mapper) {
        return PageUtil.toPage(page.map(mapper::toDto));
    }

    /**
     * 分页查询并转换为分页结果（自定义转换）
     * @param page 分页数据
     * @param converter 转换函数
     * @return 分页结果
     */
    public static <D, E> PageResult<D> toPageResult(Page<E> page, Function<E, D> converter) {
        return PageUtil.toPage(page.map(converter));
    }

    /**
     * 获取实体ID
     */
    @SuppressWarnings("unchecked")
    private static <T, ID> ID getEntityId(T entity) {
        try {
            return (ID) entity.getClass().getMethod("getId").invoke(entity);
        } catch (Exception e) {
            return null;
        }
    }
}
