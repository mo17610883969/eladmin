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
   * 验证是否为邮箱
   */
  public static boolean isEmail(String email) {
    return Validator.isEmail(email);
  }

    /**
     * 验证新实体不应有ID
     *
     * @param id 实体ID
     * @param entityName 实体名称
     */
    public static void validateNewEntity(Long id, String entityName) {
        if (id != null) {
            throw new BadRequestException("A new " + entityName + " cannot already have an ID");
        }
    }

    /**
     * 根据ID获取实体并验证存在性
     *
     * @param id 实体ID
     * @param entitySupplier 实体获取函数
     * @param entityName 实体名称
     * @param <T> 实体类型
     * @return 实体对象
     */
    public static <T> T getEntityById(Long id, Supplier<T> entitySupplier, String entityName) {
        T entity = entitySupplier.get();
        try {
            java.lang.reflect.Method getIdMethod = entity.getClass().getMethod("getId");
            Object entityId = getIdMethod.invoke(entity);
            isNull(entityId, entityName, "id", id);
        } catch (Exception e) {
            throw new BadRequestException(entityName + " 不存在: id is " + id);
        }
        return entity;
    }
}
