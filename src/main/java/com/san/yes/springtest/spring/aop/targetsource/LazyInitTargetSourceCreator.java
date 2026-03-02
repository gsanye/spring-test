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

package org.springframework.aop.framework.autoproxy.target;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * 为每个定义“lazy-init”的bean创建一个LazyInitTargetSource，
 * 这将导致每个bean都会创建一个proxy，从而允许在不实际初始化这些目标bean实例的情况下，获取对这些bean的引用
 *
 * 该类需要注册为自定义的TargetSourceCreator，以便与auto-proxy creator结合使用，
 * 同时结合自定义拦截器，用于特定bean或仅用于创建lazy-init proxy
 */
public class LazyInitTargetSourceCreator extends AbstractBeanFactoryBasedTargetSourceCreator {

	@Override
	protected boolean isPrototypeBased() {
		// 非原型bean
		return false;
	}

	@Override
	protected @Nullable AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(
			Class<?> beanClass, String beanName) {

		if (getBeanFactory() instanceof ConfigurableListableBeanFactory clbf) {
			BeanDefinition definition = clbf.getBeanDefinition(beanName);
			// 判断bean-definitin的lazy-init属性
			if (definition.isLazyInit()) {
				// 返回lazy-init 的 targetSource
				return new LazyInitTargetSource();
			}
		}
		return null;
	}

}
