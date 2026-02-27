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

package com.san.yes.springtest.spring.postprocessor;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;

/**
 * 工厂钩子，允许自定义修改bean实例，如接口标记或代理等
 *
 * 通常，post-processors
 * 填充（populate）bean 通过实现：postProcessBeforeInitialization 方法
 * 代理（proxies）bean 通过实现：postProcessAfterInitialization 方法
 *
 * 注册：
 * ApplicationContext 可以自动发现所有 BeanPostProcessor定义，
 * 并且应用这些post-processors到所有后续创建的bean
 *
 * 排序：
 * PriorityOrdered、Ordered、注册顺序 进行排序，应用到BeanFactory
 *
 */
public interface BeanPostProcessor {

	/**
	 * 在bean实例（instance）初始化回调（initialization callbacks ：postConstruct、afterPropertiesSet、init-method）之前应用
	 * 此时bean已经装配完成（populated）
	 */
	default @Nullable Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * 在bean实例化（instance）初始化回调（initialization callbacks: postConstruct、afterPropertiesSet、destroy-method）之后应用
	 * 此时bean已经装配完成（populated）
	 */
	default @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
