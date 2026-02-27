/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.san.yes.springtest.spring.bfpp;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;

/**
 * Factory的回调，允许自定义修改application context实例的bean factory、definition、 property values
 * 常用于自定义配置文件覆盖context中的配置，如：PropertyResourceConfig
 * BFPP只能用于修改bean-definition，不能用于修改bean-instance，否则可能导致bean过早初始化、破坏容器引发其他问题
 * 注册：Application-Context可以自动发现BFPP的bean-definition，也可以手动注册BFPP实例到context
 * 排序：手动注册的到context的BFPP根据注册顺序执行，context自动发现的根据PriorityOrdered、Ordered、其他顺序执行
 *
 */
@FunctionalInterface
public interface BeanFactoryPostProcessor {
	/**
	 * 修改context的内部BeanFactory,所有的bean definition都已经加载但是未实例化
	 * 可以修改或者添加属性，甚至提前初始化bean
	 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;
}
