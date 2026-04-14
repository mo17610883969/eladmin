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
package me.zhengjie.base;

import me.zhengjie.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;

/**
 * 通用服务接口
 * @author Zheng Jie
 * @date 2019年10月24日20:46:32
 */
public interface BaseService<D, E, ID, Q> {

    /**
     * 分页查询
     * @param criteria 查询条件
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<D> queryAll(Q criteria, Pageable pageable);

    /**
     * 查询全部
     * @param criteria 查询条件
     * @return 列表
     */
    List<D> queryAll(Q criteria);

    /**
     * 根据ID查询
     * @param id ID
     * @return DTO
     */
    D findById(ID id);

    /**
     * 创建
     * @param resources 实体
     */
    void create(E resources);

    /**
     * 更新
     * @param resources 实体
     */
    void update(E resources);

    /**
     * 删除
     * @param ids ID集合
     */
    void delete(Set<ID> ids);

    /**
     * 导出
     * @param queryAll 数据列表
     * @param response 响应
     * @throws IOException IO异常
     */
    void download(List<D> queryAll, HttpServletResponse response) throws IOException;
}
