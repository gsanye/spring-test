package com.san.yes.springtest.bean.configuration;

import com.san.yes.springtest.bean.LifeCircleBean;
import com.san.yes.springtest.bean.factory.MyFactoryBean;
import com.san.yes.springtest.bean.registrar.MyImportBeanDefinitionRegistrar;
import com.san.yes.springtest.bean.Person;
import com.san.yes.springtest.bean.selector.MyImportSelector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({Person.class, MyImportSelector.class, MyImportBeanDefinitionRegistrar.class})
@Configuration
public class MainConfiguration {

    @Bean
    public MyFactoryBean myFactoryBean() {
        return new MyFactoryBean();
    }
    @Bean(initMethod = "initMethod",destroyMethod = "destroyMethod")
    public LifeCircleBean lifeCircleBean() {
        return new LifeCircleBean();
    }
}
