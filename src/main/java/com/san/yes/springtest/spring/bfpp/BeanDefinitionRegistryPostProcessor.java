
package com.san.yes.springtest.spring.bfpp;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * 扩展标展BFPP，允许在BFPP被检测启动前注册更多的bean-definition
 * 通常用来注册更多的bean-definition
 * 这些bean-definition可能是BeanDefinitionRegistryPostProcessor，这些definition又定义了其他BFPP
 */
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {

	/**
	 * 修改 application-context的bean-definition-registry，所有的bean-definition已经加载，但是尚未初始化
	 * 允许在BFPP前添加更多bean-definition
	 */
	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;
}
