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

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.template.*;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.domain.GenConfig;
import me.zhengjie.domain.ColumnInfo;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.util.*;

import static me.zhengjie.utils.FileUtil.SYS_TEM_DIR;

/**
 * 代码生成
 *
 * @author Zheng Jie
 * @date 2019-01-02
 */
@Slf4j
@SuppressWarnings({"unchecked", "all"})
public class GenUtil {

    private static final String TIMESTAMP = "Timestamp";

    private static final String BIGDECIMAL = "BigDecimal";

    public static final String PK = "PRI";

    public static final String EXTRA = "auto_increment";

    /**
     * 获取后端代码模板名称
     *
     * @return List
     */
    private static List<String> getAdminTemplateNames() {
        List<String> templateNames = new ArrayList<>();
        templateNames.add("Entity");
        templateNames.add("Dto");
        templateNames.add("Mapper");
        templateNames.add("Controller");
        templateNames.add("QueryCriteria");
        templateNames.add("Service");
        templateNames.add("ServiceImpl");
        templateNames.add("Repository");
        return templateNames;
    }

    /**
     * 获取前端代码模板名称
     *
     * @return List
     */
    private static List<String> getFrontTemplateNames() {
        List<String> templateNames = new ArrayList<>();
        templateNames.add("index");
        templateNames.add("api");
        return templateNames;
    }

    public static List<Map<String, Object>> preview(List<ColumnInfo> columns, GenConfig genConfig) {
        Map<String, Object> genMap = getGenMap(columns, genConfig);
        List<Map<String, Object>> genList = new ArrayList<>();
        // 获取后端模版
        List<String> templates = getAdminTemplateNames();
        TemplateEngine engine = TemplateUtil.createEngine(new TemplateConfig("template", TemplateConfig.ResourceMode.CLASSPATH));
        for (String templateName : templates) {
            Map<String, Object> map = new HashMap<>(1);
            Template template = engine.getTemplate("admin/" + templateName + ".ftl");
            map.put("content", template.render(genMap));
            map.put("name", templateName);
            genList.add(map);
        }
        // 获取前端模版
        templates = getFrontTemplateNames();
        for (String templateName : templates) {
            Map<String, Object> map = new HashMap<>(1);
            Template template = engine.getTemplate("front/" + templateName + ".ftl");
            map.put(templateName, template.render(genMap));
            map.put("content", template.render(genMap));
            map.put("name", templateName);
            genList.add(map);
        }
        return genList;
    }

    public static String download(List<ColumnInfo> columns, GenConfig genConfig) throws IOException {
        // 拼接的路径：/tmpeladmin-gen-temp/，这个路径在Linux下需要root用户才有权限创建,非root用户会权限错误而失败，更改为： /tmp/eladmin-gen-temp/
        // String tempPath =SYS_TEM_DIR + "eladmin-gen-temp" + File.separator + genConfig.getTableName() + File.separator;
        String tempPath = SYS_TEM_DIR + "eladmin-gen-temp" + File.separator + genConfig.getTableName() + File.separator;
        Map<String, Object> genMap = getGenMap(columns, genConfig);
        TemplateEngine engine = TemplateUtil.createEngine(new TemplateConfig("template", TemplateConfig.ResourceMode.CLASSPATH));
        // 生成后端代码
        List<String> templates = getAdminTemplateNames();
        for (String templateName : templates) {
            Template template = engine.getTemplate("admin/" + templateName + ".ftl");
            String filePath = getAdminFilePath(templateName, genConfig, genMap.get("className").toString(), tempPath + "eladmin" + File.separator);
            assert filePath != null;
            File file = new File(filePath);
            // 如果非覆盖生成
            if (!genConfig.getCover() && FileUtil.exist(file)) {
                continue;
            }
            // 生成代码
            genFile(file, template, genMap);
        }
        // 生成前端代码
        templates = getFrontTemplateNames();
        for (String templateName : templates) {
            Template template = engine.getTemplate("front/" + templateName + ".ftl");
            String path = tempPath + "eladmin-web" + File.separator;
            String apiPath = path + "src" + File.separator + "api" + File.separator;
            String srcPath = path + "src" + File.separator + "views" + File.separator + genMap.get("changeClassName").toString() + File.separator;
            String filePath = getFrontFilePath(templateName, apiPath, srcPath, genMap.get("changeClassName").toString());
            assert filePath != null;
            File file = new File(filePath);
            // 如果非覆盖生成
            if (!genConfig.getCover() && FileUtil.exist(file)) {
                continue;
            }
            // 生成代码
            genFile(file, template, genMap);
        }
        return tempPath;
    }

    public static void generatorCode(List<ColumnInfo> columnInfos, GenConfig genConfig) throws IOException {
        Map<String, Object> genMap = getGenMap(columnInfos, genConfig);
        TemplateEngine engine = TemplateUtil.createEngine(new TemplateConfig("template", TemplateConfig.ResourceMode.CLASSPATH));
        // 生成后端代码
        List<String> templates = getAdminTemplateNames();
        for (String templateName : templates) {
            Template template = engine.getTemplate("admin/" + templateName + ".ftl");
            String rootPath = System.getProperty("user.dir");
            String filePath = getAdminFilePath(templateName, genConfig, genMap.get("className").toString(), rootPath);

            assert filePath != null;
            File file = new File(filePath);

            // 如果非覆盖生成
            if (!genConfig.getCover() && FileUtil.exist(file)) {
                continue;
            }
            // 生成代码
            genFile(file, template, genMap);
        }

        // 生成前端代码
        templates = getFrontTemplateNames();
        for (String templateName : templates) {
            Template template = engine.getTemplate("front/" + templateName + ".ftl");
            String filePath = getFrontFilePath(templateName, genConfig.getApiPath(), genConfig.getPath(), genMap.get("changeClassName").toString());

            assert filePath != null;
            File file = new File(filePath);

            // 如果非覆盖生成
            if (!genConfig.getCover() && FileUtil.exist(file)) {
                continue;
            }
            // 生成代码
            genFile(file, template, genMap);
        }
    }

    // 获取模版数据
    private static Map<String, Object> getGenMap(List<ColumnInfo> columnInfos, GenConfig genConfig) {
        Map<String, Object> genMap = initGenMap(genConfig);
        List<Map<String, Object>> columns = new ArrayList<>();
        List<Map<String, Object>> queryColumns = new ArrayList<>();
        List<String> dicts = new ArrayList<>();
        List<Map<String, Object>> betweens = new ArrayList<>();
        List<Map<String, Object>> isNotNullColumns = new ArrayList<>();

        for (ColumnInfo column : columnInfos) {
            Map<String, Object> listMap = processColumn(column, genMap, dicts);
            String colType = (String) listMap.get("_colType");
            
            if (column.getNotNull()) {
                isNotNullColumns.add(listMap);
            }
            
            processQueryColumn(column, listMap, colType, genMap, queryColumns, betweens);
            columns.add(listMap);
        }
        
        genMap.put("columns", columns);
        genMap.put("queryColumns", queryColumns);
        genMap.put("dicts", dicts);
        genMap.put("betweens", betweens);
        genMap.put("isNotNullColumns", isNotNullColumns);
        return genMap;
    }

    private static Map<String, Object> initGenMap(GenConfig genConfig) {
        Map<String, Object> genMap = new HashMap<>(16);
        genMap.put("apiAlias", genConfig.getApiAlias());
        genMap.put("package", genConfig.getPack());
        genMap.put("moduleName", genConfig.getModuleName());
        genMap.put("author", genConfig.getAuthor());
        genMap.put("date", LocalDate.now().toString());
        genMap.put("tableName", genConfig.getTableName());
        
        String[] classNames = generateClassNames(genConfig);
        genMap.put("className", classNames[0]);
        genMap.put("changeClassName", classNames[1]);
        
        initFlags(genMap);
        return genMap;
    }

    private static String[] generateClassNames(GenConfig genConfig) {
        String className = StringUtils.toCapitalizeCamelCase(genConfig.getTableName());
        String changeClassName = StringUtils.toCamelCase(genConfig.getTableName());
        
        if (StringUtils.isNotEmpty(genConfig.getPrefix())) {
            className = StringUtils.toCapitalizeCamelCase(StrUtil.removePrefix(genConfig.getTableName(), genConfig.getPrefix()));
            changeClassName = StringUtils.toCamelCase(StrUtil.removePrefix(genConfig.getTableName(), genConfig.getPrefix()));
            changeClassName = StringUtils.uncapitalize(changeClassName);
        }
        return new String[]{className, changeClassName};
    }

    private static void initFlags(Map<String, Object> genMap) {
        genMap.put("hasTimestamp", false);
        genMap.put("queryHasTimestamp", false);
        genMap.put("hasBigDecimal", false);
        genMap.put("queryHasBigDecimal", false);
        genMap.put("hasQuery", false);
        genMap.put("auto", false);
        genMap.put("hasDict", false);
        genMap.put("hasDateAnnotation", false);
    }

    private static Map<String, Object> processColumn(ColumnInfo column, Map<String, Object> genMap, List<String> dicts) {
        Map<String, Object> listMap = new HashMap<>(16);
        listMap.put("remark", column.getRemark());
        listMap.put("columnKey", column.getKeyType());
        
        String colType = ColUtil.cloToJava(column.getColumnType());
        String changeColumnName = StringUtils.toCamelCase(column.getColumnName());
        String capitalColumnName = StringUtils.toCapitalizeCamelCase(column.getColumnName());
        
        processPrimaryKey(column, colType, changeColumnName, capitalColumnName, genMap);
        processColumnType(colType, genMap);
        processAutoIncrement(column, genMap);
        processDict(column, genMap, dicts);
        
        listMap.put("columnType", colType);
        listMap.put("columnName", column.getColumnName());
        listMap.put("istNotNull", column.getNotNull());
        listMap.put("columnShow", column.getListShow());
        listMap.put("formShow", column.getFormShow());
        listMap.put("formType", StringUtils.isNotBlank(column.getFormType()) ? column.getFormType() : "Input");
        listMap.put("changeColumnName", changeColumnName);
        listMap.put("capitalColumnName", capitalColumnName);
        listMap.put("dictName", column.getDictName());
        listMap.put("dateAnnotation", column.getDateAnnotation());
        
        processDateAnnotation(column, genMap);
        
        listMap.put("_colType", colType);
        return listMap;
    }

    private static void processPrimaryKey(ColumnInfo column, String colType, String changeColumnName, String capitalColumnName, Map<String, Object> genMap) {
        if (PK.equals(column.getKeyType())) {
            genMap.put("pkColumnType", colType);
            genMap.put("pkChangeColName", changeColumnName);
            genMap.put("pkCapitalColName", capitalColumnName);
        }
    }

    private static void processColumnType(String colType, Map<String, Object> genMap) {
        if (TIMESTAMP.equals(colType)) {
            genMap.put("hasTimestamp", true);
        }
        if (BIGDECIMAL.equals(colType)) {
            genMap.put("hasBigDecimal", true);
        }
    }

    private static void processAutoIncrement(ColumnInfo column, Map<String, Object> genMap) {
        if (EXTRA.equals(column.getExtra())) {
            genMap.put("auto", true);
        }
    }

    private static void processDict(ColumnInfo column, Map<String, Object> genMap, List<String> dicts) {
        if (StringUtils.isNotBlank(column.getDictName())) {
            genMap.put("hasDict", true);
            if (!dicts.contains(column.getDictName())) {
                dicts.add(column.getDictName());
            }
        }
    }

    private static void processDateAnnotation(ColumnInfo column, Map<String, Object> genMap) {
        if (StringUtils.isNotBlank(column.getDateAnnotation())) {
            genMap.put("hasDateAnnotation", true);
        }
    }

    private static void processQueryColumn(ColumnInfo column, Map<String, Object> listMap, String colType, 
                                          Map<String, Object> genMap, List<Map<String, Object>> queryColumns, 
                                          List<Map<String, Object>> betweens) {
        if (StringUtils.isBlank(column.getQueryType())) {
            return;
        }
        
        listMap.put("queryType", column.getQueryType());
        genMap.put("hasQuery", true);
        
        if (TIMESTAMP.equals(colType)) {
            genMap.put("queryHasTimestamp", true);
        }
        if (BIGDECIMAL.equals(colType)) {
            genMap.put("queryHasBigDecimal", true);
        }
        
        if ("between".equalsIgnoreCase(column.getQueryType())) {
            betweens.add(listMap);
        } else {
            queryColumns.add(listMap);
        }
    }

    /**
     * 定义后端文件路径以及名称
     */
    private static String getAdminFilePath(String templateName, GenConfig genConfig, String className, String rootPath) {
        String projectPath = rootPath + File.separator + genConfig.getModuleName();
        String packagePath = projectPath + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;
        if (!ObjectUtils.isEmpty(genConfig.getPack())) {
            packagePath += genConfig.getPack().replace(".", File.separator) + File.separator;
        }

        if ("Entity".equals(templateName)) {
            return packagePath + "domain" + File.separator + className + ".java";
        }

        if ("Controller".equals(templateName)) {
            return packagePath + "rest" + File.separator + className + "Controller.java";
        }

        if ("Service".equals(templateName)) {
            return packagePath + "service" + File.separator + className + "Service.java";
        }

        if ("ServiceImpl".equals(templateName)) {
            return packagePath + "service" + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
        }

        if ("Dto".equals(templateName)) {
            return packagePath + "service" + File.separator + "dto" + File.separator + className + "Dto.java";
        }

        if ("QueryCriteria".equals(templateName)) {
            return packagePath + "service" + File.separator + "dto" + File.separator + className + "QueryCriteria.java";
        }

        if ("Mapper".equals(templateName)) {
            return packagePath + "service" + File.separator + "mapstruct" + File.separator + className + "Mapper.java";
        }

        if ("Repository".equals(templateName)) {
            return packagePath + "repository" + File.separator + className + "Repository.java";
        }

        return null;
    }

    /**
     * 定义前端文件路径以及名称
     */
    private static String getFrontFilePath(String templateName, String apiPath, String path, String apiName) {

        if ("api".equals(templateName)) {
            return apiPath + File.separator + apiName + ".js";
        }

        if ("index".equals(templateName)) {
            return path + File.separator + "index.vue";
        }

        return null;
    }

    private static void genFile(File file, Template template, Map<String, Object> map) throws IOException {
        // 生成目标文件
        Writer writer = null;
        try {
            FileUtil.touch(file);
            writer = new FileWriter(file);
            template.render(map, writer);
        } catch (TemplateException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            assert writer != null;
            writer.close();
        }
    }
}
