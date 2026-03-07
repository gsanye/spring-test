package com.san.yes.springtest.aop.configuration;

import com.san.yes.springtest.aop.advice.MyMethodInteceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class AopConfiguration {
    @Bean
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        // 自动代理创建器
        return new DefaultAdvisorAutoProxyCreator();
    }

    @Bean
    public Advisor customAdvisor(MyMethodInteceptor myMethodInteceptor) {
        // 方法名称匹配切点
        NameMatchMethodPointcutAdvisor nameMatchMethodPointcutAdvisor = new NameMatchMethodPointcutAdvisor();
        // 指定匹配的方法规则，即使为私有方法也会匹配到，然后创建代理对象
        nameMatchMethodPointcutAdvisor.addMethodName("*doHello");
        // 指定advice通知
        nameMatchMethodPointcutAdvisor.setAdvice(myMethodInteceptor);
        return nameMatchMethodPointcutAdvisor;
    }

    @Bean
    public MyMethodInteceptor myMethodInteceptor() {
        // 方法拦截器
        return new MyMethodInteceptor();
    }
}
