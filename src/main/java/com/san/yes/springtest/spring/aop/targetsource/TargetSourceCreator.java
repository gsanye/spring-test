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

package org.springframework.aop.framework.autoproxy;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.TargetSource;

/**
 * 实现可以为特定的bean，创建特殊的目标源target source，如：池化的目标源。
 * 可以根据目标类的某些属性（如池化属性）做出选择
 * AbstractAutoProxyCreator可以支持多个TargetSourceCreator，按照顺序应用
 */
@FunctionalInterface
public interface TargetSourceCreator {

	/**
	 * 为给定的 bean（如果有的话）创建一个特殊的 TargetSource。
	 */
	@Nullable TargetSource getTargetSource(Class<?> beanClass, String beanName);

}
