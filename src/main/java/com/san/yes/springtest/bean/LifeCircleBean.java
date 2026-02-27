package com.san.yes.springtest.bean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class LifeCircleBean implements InitializingBean, DisposableBean {

    /**
     * java 规范注解，
     * 执行时机：最先执行
     */
    @PostConstruct
    public void postConstructMethod() {
        System.out.println("postConstruct method executed");
    }

    /**
     * java 规范注解，
     * 执行时机：最先执行
     */
    @PreDestroy
    public void preDestroyMethod() {
        System.out.println("preDestroy method executed");
    }

    /**
     * DisposableBean 接口定义方法
     */
    @Override
    public void destroy() throws Exception {
        System.out.println("disposable bean destroy method executed");
    }

    /**
     * InitializingBean 接口定义方法
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("initializing bean after properties set method executed");
    }

    /**
     * xml 或 @Bean 中指定初始化方法，
     * 执行时机：最后执行
     */
    public void initMethod() {
        System.out.println("init method executed");
    }

    /**
     * xml 或 @Bean 中指定初始化方法，
     * 执行时机：最后执行
     */
    public void destroyMethod() {
        System.out.println("destroy method executed");
    }

}
