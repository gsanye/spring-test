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
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;

/**
 * 添加了before-instantiation 和 after-instantiation （显示属性装配、自动注入前）的回调方法
 * 通常：
 * 用于抑制特殊目标bean的默认实例化，比如创建特殊TargetSource（池化pooling、延迟初始化 lazing initializing等）
 * 或实现特殊的注入inject
 * 注意：此类通常由框架继承实现
 */
public interface InstantiationAwareBeanPostProcessor extends org.springframework.beans.factory.config.BeanPostProcessor {

	/**
	 * bean实例化之前，返回代理对象代替目标bean，抑制默认实例化
	 * 返回non-null对象，bean的创建creation流程被短路，仅执行 postProcessAfterInitialization 方法。
	 */
	default @Nullable Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * 在bean实例化之后，属性填充（属性填充或依赖注入）之前
	 * 自定义属性注入的回调方法
	 * 返回false跳过后续所有 InstantiationAwareBeanPostProcessor
	 */
	default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	/**
	 * 在应用到bean之前，对属性值进行后置处理
	 */
	default @Nullable PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
			throws BeansException {
		return pvs;
	}

}
