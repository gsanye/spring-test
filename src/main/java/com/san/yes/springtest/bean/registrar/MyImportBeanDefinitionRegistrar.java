package com.san.yes.springtest.bean.registrar;

import com.san.yes.springtest.bean.Color;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class MyImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(Color.class)
                .addPropertyValue("color", "registerByImportBeanDefinitionRegistrar");
        registry.registerBeanDefinition("registerByImportBeanDefinitionRegistrar",beanDefinitionBuilder.getBeanDefinition());
    }
}
