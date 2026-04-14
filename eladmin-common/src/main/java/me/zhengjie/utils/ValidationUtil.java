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

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import me.zhengjie.exception.BadRequestException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * 验证工具
 *
 * @author Zheng Jie
 * @date 2018-11-23
 */
public class ValidationUtil {

    /**
     * 验证空
     */
    public static void isNull(Object obj, String entity, String parameter , Object value){
        if(ObjectUtil.isNull(obj)){
            String msg = entity + " 不存在: "+ parameter +" is "+ value;
            throw new BadRequestException(msg);
        }
    }

    /**
     * 验证实体是否存在
     */
    public static void checkEntityExists(Object entityId, String entityName, Object id) {
        isNull(entityId, entityName, "id", id);
    }

    /**
     * 通过ID查找并验证实体
     */
    public static <T, ID> T getByIdOrThrow(JpaRepository<T, ID> repository, ID id, String entityName, Supplier<T> newInstanceSupplier) {
        T entity = repository.findById(id).orElseGet(newInstanceSupplier);
        try {
            Method getIdMethod = entity.getClass().getMethod("getId");
            Object entityId = getIdMethod.invoke(entity);
            checkEntityExists(entityId, entityName, id);
        } catch (Exception e) {
            throw new BadRequestException(entityName + " 不存在: id is " + id);
        }
        return entity;
    }

  /**
   * 验证是否为邮箱
   */
  public static boolean isEmail(String email) {
    return Validator.isEmail(email);
  }
}
