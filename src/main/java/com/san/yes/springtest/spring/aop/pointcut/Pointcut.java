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

package org.springframework.aop;

/**
 * Spring核心切面抽象
 * 切面由ClassFilter和MethodFilter组成。
 * 这些基本术语以及切面本身可以通过组合（如：ComposablePointcut）来构建组合
 */
public interface Pointcut {

	/**
	 * ClassFilter类过滤器:哪些类需要拦截
	 */
	ClassFilter getClassFilter();

	/**
	 * MethodMatcher方法匹配器：哪些方法需要拦截
	 */
	MethodMatcher getMethodMatcher();

	/**
	 * 匹配所有对向的Pointcut切点实例
	 */
	Pointcut TRUE = TruePointcut.INSTANCE;

}
