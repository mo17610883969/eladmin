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
package me.zhengjie.modules.quartz.utils;

import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import me.zhengjie.domain.vo.EmailVo;
import me.zhengjie.modules.quartz.domain.QuartzJob;
import me.zhengjie.modules.quartz.domain.QuartzLog;
import me.zhengjie.modules.quartz.repository.QuartzLogRepository;
import me.zhengjie.modules.quartz.service.QuartzJobService;
import me.zhengjie.service.EmailService;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.utils.SpringBeanHolder;
import me.zhengjie.utils.StringUtils;
import me.zhengjie.utils.ThrowableUtil;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.quartz.QuartzJobBean;
import java.util.*;
import java.util.concurrent.*;

/**
 * 参考人人开源，<a href="https://gitee.com/renrenio/renren-security">...</a>
 * @author /
 * @date 2019-01-07
 */
public class ExecutionJob extends QuartzJobBean {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    // 此处仅供参考，可根据任务执行情况自定义线程池参数
    private final ThreadPoolTaskExecutor executor = SpringBeanHolder.getBean("taskAsync");


    @Override
    public void executeInternal(JobExecutionContext context) {
        QuartzJob quartzJob = getQuartzJob(context);
        JobBeans beans = getJobBeans();
        
        String uuid = quartzJob.getUuid();
        QuartzLog log = initQuartzLog(quartzJob);
        long startTime = System.currentTimeMillis();
        
        try {
            executeTask(quartzJob, log, startTime);
            handleSuccess(quartzJob, beans, uuid, log, startTime);
        } catch (Exception e) {
            handleFailure(quartzJob, beans, uuid, log, startTime, e);
        } finally {
            beans.quartzLogRepository.save(log);
        }
    }

    private QuartzJob getQuartzJob(JobExecutionContext context) {
        return (QuartzJob) context.getMergedJobDataMap().get(QuartzJob.JOB_KEY);
    }

    private JobBeans getJobBeans() {
        QuartzLogRepository quartzLogRepository = SpringBeanHolder.getBean(QuartzLogRepository.class);
        QuartzJobService quartzJobService = SpringBeanHolder.getBean(QuartzJobService.class);
        RedisUtils redisUtils = SpringBeanHolder.getBean(RedisUtils.class);
        return new JobBeans(quartzLogRepository, quartzJobService, redisUtils);
    }

    private QuartzLog initQuartzLog(QuartzJob quartzJob) {
        QuartzLog log = new QuartzLog();
        log.setJobName(quartzJob.getJobName());
        log.setBeanName(quartzJob.getBeanName());
        log.setMethodName(quartzJob.getMethodName());
        log.setParams(quartzJob.getParams());
        log.setCronExpression(quartzJob.getCronExpression());
        return log;
    }

    private void executeTask(QuartzJob quartzJob, QuartzLog log, long startTime) throws Exception {
        QuartzRunnable task = new QuartzRunnable(quartzJob.getBeanName(), quartzJob.getMethodName(), quartzJob.getParams());
        Future<?> future = executor.submit(task);
        future.get();
        long times = System.currentTimeMillis() - startTime;
        log.setTime(times);
        log.setIsSuccess(true);
        logger.info("任务执行成功，任务名称：{}, 执行时间：{}毫秒", quartzJob.getJobName(), times);
    }

    private void handleSuccess(QuartzJob quartzJob, JobBeans beans, String uuid, QuartzLog log, long startTime) {
        if (StringUtils.isNotBlank(uuid)) {
            beans.redisUtils.set(uuid, true);
        }
        executeSubTasks(quartzJob, beans);
    }

    private void executeSubTasks(QuartzJob quartzJob, JobBeans beans) {
        if (StringUtils.isBlank(quartzJob.getSubTask())) {
            return;
        }
        String[] tasks = quartzJob.getSubTask().split("[,，]");
        try {
            beans.quartzJobService.executionSubJob(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("执行子任务被中断", e);
        }
    }

    private void handleFailure(QuartzJob quartzJob, JobBeans beans, String uuid, QuartzLog log, long startTime, Exception e) {
        updateRedisStatus(uuid, beans);
        updateLogForFailure(log, startTime, e);
        logger.error("任务执行失败，任务名称：{}", quartzJob.getJobName());
        
        handlePauseAfterFailure(quartzJob, beans);
        sendAlarmEmail(quartzJob, e);
    }

    private void updateRedisStatus(String uuid, JobBeans beans) {
        if (StringUtils.isNotBlank(uuid)) {
            beans.redisUtils.set(uuid, false);
        }
    }

    private void updateLogForFailure(QuartzLog log, long startTime, Exception e) {
        long times = System.currentTimeMillis() - startTime;
        log.setTime(times);
        log.setIsSuccess(false);
        log.setExceptionDetail(ThrowableUtil.getStackTrace(e));
    }

    private void handlePauseAfterFailure(QuartzJob quartzJob, JobBeans beans) {
        if (quartzJob.getPauseAfterFailure() == null || !quartzJob.getPauseAfterFailure()) {
            return;
        }
        quartzJob.setIsPause(false);
        beans.quartzJobService.updateIsPause(quartzJob);
    }

    private void sendAlarmEmail(QuartzJob quartzJob, Exception e) {
        if (quartzJob.getEmail() == null || StringUtils.isBlank(quartzJob.getEmail())) {
            return;
        }
        EmailService emailService = SpringBeanHolder.getBean(EmailService.class);
        EmailVo emailVo = taskAlarm(quartzJob, ThrowableUtil.getStackTrace(e));
        emailService.send(emailVo, emailService.find());
    }

    private EmailVo taskAlarm(QuartzJob quartzJob, String msg) {
        EmailVo emailVo = new EmailVo();
        emailVo.setSubject("定时任务【"+ quartzJob.getJobName() +"】执行失败，请尽快处理！");
        Map<String, Object> data = new HashMap<>(16);
        data.put("task", quartzJob);
        data.put("msg", msg);
        TemplateEngine engine = TemplateUtil.createEngine(new TemplateConfig("template", TemplateConfig.ResourceMode.CLASSPATH));
        Template template = engine.getTemplate("taskAlarm.ftl");
        emailVo.setContent(template.render(data));
        List<String> emails = Arrays.asList(quartzJob.getEmail().split("[,，]"));
        emailVo.setTos(emails);
        return emailVo;
    }

    private static class JobBeans {
        final QuartzLogRepository quartzLogRepository;
        final QuartzJobService quartzJobService;
        final RedisUtils redisUtils;

        JobBeans(QuartzLogRepository quartzLogRepository, QuartzJobService quartzJobService, RedisUtils redisUtils) {
            this.quartzLogRepository = quartzLogRepository;
            this.quartzJobService = quartzJobService;
            this.redisUtils = redisUtils;
        }
    }
}
