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
import me.zhengjie.utils.PageUtil;
import me.zhengjie.utils.QueryHelp;
import me.zhengjie.utils.ValidationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletResponse;

/**
 * 通用服务实现类
 * 提供常见的CRUD操作默认实现，减少重复代码
 * @author Zheng Jie
 * @date 2019年10月24日20:46:32
 */
public abstract class AbstractBaseService<D, E, ID, Q, R extends JpaRepository<E, ID> & JpaSpecificationExecutor<E>, M extends BaseMapper<D, E>> 
        implements BaseService<D, E, ID, Q> {

    protected abstract R getRepository();
    
    protected abstract M getMapper();
    
    protected abstract String getEntityName();
    
    protected abstract Supplier<E> getEntityConstructor();

    @Override
    public PageResult<D> queryAll(Q criteria, Pageable pageable) {
        Page<E> page = getRepository().findAll(
                (root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root, criteria, criteriaBuilder), 
                pageable);
        return PageUtil.toPage(page.map(getMapper()::toDto));
    }

    @Override
    public List<D> queryAll(Q criteria) {
        List<E> list = getRepository().findAll(
                (root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root, criteria, criteriaBuilder));
        return getMapper().toDto(list);
    }

    @Override
    public D findById(ID id) {
        E entity = getRepository().findById(id).orElseGet(getEntityConstructor());
        ValidationUtil.isNull(getEntityId(entity), getEntityName(), "id", id);
        return getMapper().toDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(E resources) {
        getRepository().save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(E resources) {
        E entity = getRepository().findById(getEntityId(resources)).orElseGet(getEntityConstructor());
        ValidationUtil.isNull(getEntityId(entity), getEntityName(), "id", getEntityId(resources));
        copyProperties(resources, entity);
        getRepository().save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<ID> ids) {
        for (ID id : ids) {
            getRepository().deleteById(id);
        }
    }

    @Override
    public void download(List<D> queryAll, HttpServletResponse response) throws IOException {
    }

    protected void copyProperties(E source, E target) {
    }

    protected abstract ID getEntityId(E entity);
}
