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

package org.springframework.aop.framework;

import org.jspecify.annotations.Nullable;

/**
 * 配置的AOP proxy委托接口，允许创建实际的代理对象
 * 开箱即用的可用实现：1.JDK动态代理 2.CGLIB代理。由DefaultAopProxyFactory创建
 */
public interface AopProxy {

	/**
	 * 创建一个新的代理对象
	 * 使用AopProxy的默认类加载器（如果代理创建需要的话）：通常，使用的为thread context class loader
	 */
	Object getProxy();

	/**
	 * 创建一个新的代理对象
	 * 使用给定class loader，如果为null，则使用默认逻辑
	 */
	Object getProxy(@Nullable ClassLoader classLoader);

	/**
	 * 确定代理class
	 */
	Class<?> getProxyClass(@Nullable ClassLoader classLoader);

}
