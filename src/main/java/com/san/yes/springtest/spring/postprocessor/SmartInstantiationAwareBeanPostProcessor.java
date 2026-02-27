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
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;

import java.lang.reflect.Constructor;

/**
 * 扩展 InstantiationAwareBPP接口，增加类型推断回调方法
 * 注意：该接口一般为框架使用
 */
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

	/**
	 * 预测此BPP的postProcessBeforeInstantiation方法返回的bean类型
	 * 默认返回null
	 */
	default @Nullable Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * 确定该BPP的postProcessBeforeInstantiation方法返回的bean类型
	 * 默认返回beanClass
	 */
	default Class<?> determineBeanType(Class<?> beanClass, String beanName) throws BeansException {
		return beanClass;
	}

	/**
	 * 确定给定bean的构造器
	 * 默认返回null，无参构造
	 */
	default Constructor<?> @Nullable [] determineCandidateConstructors(Class<?> beanClass, String beanName)
			throws BeansException {
		return null;
	}

	/**
	 * 获取bean的提前引用（early reference），通常用于解决循环引用 circular reference
	 * 在完全初始化之前有机会提前暴露包装器，暴露对象应当与postProcess Before\After Initialization 方法暴露的相同。
	 * 除非后续post-processor返回不同的包装器，否则该返回作为bean的最终引用
	 */
	default Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
