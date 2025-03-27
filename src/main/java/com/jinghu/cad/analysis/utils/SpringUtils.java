package com.jinghu.cad.analysis.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author liming
 * @version 1.0
 * @description SpringUtils
 * @date 2025/3/26 15:08
 */
@Component
public class SpringUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public static <T> T getBean(Class<T> clazz) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(clazz);
    }

    // 获取环境变量值
    public static String getEnv(String key, String defaultValue) {
        if (applicationContext == null) {
            return defaultValue;
        }
        Environment env = applicationContext.getEnvironment();
        return env.getProperty(key);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtils.applicationContext = applicationContext;
    }
}
