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
        return new DefaultAdvisorAutoProxyCreator();
    }

    @Bean
    public Advisor customAdvisor(MyMethodInteceptor myMethodInteceptor) {
        NameMatchMethodPointcutAdvisor nameMatchMethodPointcutAdvisor = new NameMatchMethodPointcutAdvisor();
        // 指定匹配的方法
        nameMatchMethodPointcutAdvisor.addMethodName("*doHellox");
        // 指定advice
        nameMatchMethodPointcutAdvisor.setAdvice(myMethodInteceptor);
        return nameMatchMethodPointcutAdvisor;
    }

    @Bean
    public MyMethodInteceptor myMethodInteceptor() {
        return new MyMethodInteceptor();
    }
}
