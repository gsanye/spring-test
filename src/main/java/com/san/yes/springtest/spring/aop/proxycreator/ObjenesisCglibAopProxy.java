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

import java.lang.reflect.Constructor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.ReflectionUtils;

/**
 * 继承自CglibAopProxy基于Objenesis的扩展，用于在不调用类构造函数情况下创建proxy instance。
 * 默认使用此实现
 */
@SuppressWarnings("serial")
class ObjenesisCglibAopProxy extends CglibAopProxy {

	private static final Log logger = LogFactory.getLog(ObjenesisCglibAopProxy.class);
	// 另一种创建实例的方法，可以不用空参构造函数
	private static final SpringObjenesis objenesis = new SpringObjenesis();


	/**
	 * 为给定AOP配置创建AOPProxy实例
	 */
	public ObjenesisCglibAopProxy(AdvisedSupport config) {
		super(config);
	}


	// 创建代理类class
	@Override
	protected Class<?> createProxyClass(Enhancer enhancer) {
		return enhancer.createClass();
	}

	// 创建代理类class和实例instance
	@Override
	protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {
		// 获取代理类class
		Class<?> proxyClass = enhancer.createClass();
		Object proxyInstance = null;
		// 如果为true，那么采用objenesis新建一个实例
		if (objenesis.isWorthTrying()) {
			try {
				proxyInstance = objenesis.newInstance(proxyClass, enhancer.getUseCache());
			}
			catch (Throwable ex) {
				logger.debug("Unable to instantiate proxy using Objenesis, " +
						"falling back to regular proxy construction", ex);
			}
		}
		// 如果实例为null，则使用构造函数新建实例
		if (proxyInstance == null) {
			// Regular instantiation via default constructor...
			try {
				Constructor<?> ctor = (this.constructorArgs != null ?
						proxyClass.getDeclaredConstructor(this.constructorArgTypes) :
						proxyClass.getDeclaredConstructor());
				// 使用构造函数新建实例
				ReflectionUtils.makeAccessible(ctor);
				proxyInstance = (this.constructorArgs != null ?
						ctor.newInstance(this.constructorArgs) : ctor.newInstance());
			}
			catch (Throwable ex) {
				throw new AopConfigException("Unable to instantiate proxy using Objenesis, " +
						"and regular proxy instantiation via default constructor fails as well", ex);
			}
		}
		// 设置方法代理
		((Factory) proxyInstance).setCallbacks(callbacks);
		return proxyInstance;
	}

}
