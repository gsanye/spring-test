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

package org.springframework.beans.factory.config;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;

/**
 * 工厂钩子，允许自定义修改bean实例，如：检车标记接口或包装为代理类
 * 通常：
 * 1. 通过标记接口配置bean的处理器会实现postProcessBeforeInitialization方法
 * 2. 通过代理包装bean的处理器会实现postProcessAfterInitialization方法
 * 注册：
 * 1. Context可以自动发现BPP bean-definition
 * 2. Factory允许编程方式注册处理器
 * 排序：
 * 1. Context自动发现的bean依照：PriorityOrdered、Ordered、其他的方式排序
 * 2. Factory编程方式注册处理器：按照注册顺序，忽略ordered
 * 3. Order注解将被忽略
 */
public interface BeanPostProcessor {

	/**
	 * 在bean实例的实例化（如InitializingBean的afterPropertiesSet、init-method）被调用之前，bean已经填充了属性
	 * 返回的bean实例可能是对原始bean的包装，返回null则不会调用后续处理器
	 */
	default @Nullable Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * bean实例的实例化（如InitializingBean的afterPropertiesSet、init-method）被调用之后，bean已经填充了属性，
	 * 返回的bean实例可能是对原始bean的包装，返回null则不会调用后续处理器
	 * FactoryBean：此方法同时对FactoryBean本身和 创建的对象进行调用
	 * InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation 方法触发短路后：
	 * 此方法会被调用，BPP的其他回调方法不会
	 */
	default @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
