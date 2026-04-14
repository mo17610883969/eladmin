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
package me.zhengjie.modules.maint.service.impl;

import lombok.RequiredArgsConstructor;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.modules.maint.domain.App;
import me.zhengjie.modules.maint.repository.AppRepository;
import me.zhengjie.modules.maint.service.AppService;
import me.zhengjie.modules.maint.service.dto.AppDto;
import me.zhengjie.modules.maint.service.dto.AppQueryCriteria;
import me.zhengjie.modules.maint.service.mapstruct.AppMapper;
import me.zhengjie.utils.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
* @author zhanghouying
* @date 2019-08-24
*/
@Service
@RequiredArgsConstructor
public class AppServiceImpl implements AppService {

    private static final String ENTITY_NAME = "App";
    private static final String OPT_PATH = "/opt";
    private static final String HOME_PATH = "/home";
    private static final Set<String> FORBIDDEN_CHARS = Set.of(";", "|", "&");

    private final AppRepository appRepository;
    private final AppMapper appMapper;

    @Override
    public PageResult<AppDto> queryAll(AppQueryCriteria criteria, Pageable pageable){
        return ServiceHelper.toPageResult(
                appRepository.findAll((root, query, cb) -> QueryHelp.getPredicate(root, criteria, cb), pageable),
                appMapper);
    }

    @Override
    public List<AppDto> queryAll(AppQueryCriteria criteria){
        return appMapper.toDto(appRepository.findAll((root, query, cb) -> QueryHelp.getPredicate(root, criteria, cb)));
    }

    @Override
    public AppDto findById(Long id) {
        return ServiceHelper.findById(appRepository, appMapper, id, ENTITY_NAME, App::new);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(App resources) {
        validateAppName(resources.getName());
        validatePaths(resources);
        appRepository.save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(App resources) {
        validateAppName(resources.getName());
        validatePaths(resources);
        App app = ServiceHelper.findByIdRaw(appRepository, resources.getId(), ENTITY_NAME, App::new);
        app.copy(resources);
        appRepository.save(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<Long> ids) {
        ids.forEach(appRepository::deleteById);
    }

    @Override
    public void download(List<AppDto> queryAll, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AppDto appDto : queryAll) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("应用名称", appDto.getName());
            map.put("端口", appDto.getPort());
            map.put("上传目录", appDto.getUploadPath());
            map.put("部署目录", appDto.getDeployPath());
            map.put("备份目录", appDto.getBackupPath());
            map.put("启动脚本", appDto.getStartScript());
            map.put("部署脚本", appDto.getDeployScript());
            map.put("创建日期", appDto.getCreateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    private void validateAppName(String appName) {
        if (FORBIDDEN_CHARS.stream().anyMatch(appName::contains)) {
            throw new IllegalArgumentException("非法的应用名称，请勿包含[; | &]等特殊字符");
        }
    }

    private void validatePaths(App resources) {
        validatePath(resources.getUploadPath(), "上传");
        validatePath(resources.getDeployPath(), "部署");
        validatePath(resources.getBackupPath(), "备份");
    }

    private void validatePath(String path, String actionName) {
        if (!(path.startsWith(OPT_PATH) || path.startsWith(HOME_PATH))) {
            throw new BadRequestException("文件只能" + actionName + "在opt目录或者home目录 ");
        }
    }
}
