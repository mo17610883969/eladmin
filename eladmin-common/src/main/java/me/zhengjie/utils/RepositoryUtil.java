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

import cn.hutool.core.util.ObjectUtil;
import me.zhengjie.exception.BadRequestException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Repository工具类
 * 提供通用的Repository操作方法，减少重复代码
 * @author Zheng Jie
 * @date 2019年10月24日20:46:32
 */
public final class RepositoryUtil {

    private RepositoryUtil() {
    }

    /**
     * 根据ID查询实体，如果不存在则抛出异常
     * @param repository Repository
     * @param id ID
     * @param entityName 实体名称
     * @param constructor 实体构造函数
     * @return 实体
     */
    public static <T, ID> T findByIdOrFail(JpaRepository<T, ID> repository, ID id, String entityName, Supplier<T> constructor) {
        T entity = repository.findById(id).orElseGet(constructor);
        ID entityId = getEntityId(entity);
        if (ObjectUtil.isNull(entityId)) {
            throw new BadRequestException(entityName + " 不存在: id is " + id);
        }
        return entity;
    }

    /**
     * 根据ID查询实体，如果不存在则返回null
     * @param repository Repository
     * @param id ID
     * @return 实体或null
     */
    public static <T, ID> T findByIdOrNull(JpaRepository<T, ID> repository, ID id) {
        return repository.findById(id).orElse(null);
    }

    /**
     * 根据ID查询实体，如果不存在则使用默认值
     * @param repository Repository
     * @param id ID
     * @param defaultValue 默认值
     * @return 实体或默认值
     */
    public static <T, ID> T findByIdOrDefault(JpaRepository<T, ID> repository, ID id, T defaultValue) {
        return repository.findById(id).orElse(defaultValue);
    }

    /**
     * 根据ID查询实体，如果不存在则使用Supplier创建默认值
     * @param repository Repository
     * @param id ID
     * @param defaultSupplier 默认值Supplier
     * @return 实体或默认值
     */
    public static <T, ID> T findByIdOrGet(JpaRepository<T, ID> repository, ID id, Supplier<T> defaultSupplier) {
        return repository.findById(id).orElseGet(defaultSupplier);
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
