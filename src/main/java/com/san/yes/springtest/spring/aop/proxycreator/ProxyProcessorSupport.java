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

import java.io.Closeable;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * 此类为proxy processor 代理处理器的基类，
 * 特别是类加载器管理和evaluateProxyInterfaces算法的实现
 */
@SuppressWarnings("serial")
public class ProxyProcessorSupport extends ProxyConfig implements Ordered, BeanClassLoaderAware, AopInfrastructureBean {

	/**
	 * 确保在所有其他processor之后运行，以便他能够对现有的proxy添加advisor（通知器），而不是double-proxy双重代理
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;

	private @Nullable ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

	private boolean classLoaderConfigured = false;


	/**
	 * 设置processor的order，用于多个processor实现时使用
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 设置proxy的ClassLoader，默认为BeanFactory加载bean时的ClassLoader
	 */
	public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
		this.proxyClassLoader = classLoader;
		this.classLoaderConfigured = (classLoader != null);
	}

	/**
	 * 返回配置的ClassLoader
	 */
	protected @Nullable ClassLoader getProxyClassLoader() {
		return this.proxyClassLoader;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (!this.classLoaderConfigured) {
			this.proxyClassLoader = classLoader;
		}
	}


	/**
	 * 检查给定类上的接口，并在合适情况下将接口应用到ProxyFactory中。如果没有则回退到traget-class proxy目标类代理（CGLIB）
	 * 调用：isConfigurationCallbackInterface和isInternalLanguageInterface方法对接口进行校验
	 */
	protected void evaluateProxyInterfaces(Class<?> beanClass, ProxyFactory proxyFactory) {
		// 获取类上所有的接口
		Class<?>[] targetInterfaces = ClassUtils.getAllInterfacesForClass(beanClass, getProxyClassLoader());
		// 是否存在合适接口
		boolean hasReasonableProxyInterface = false;
		for (Class<?> ifc : targetInterfaces) {
			// 调用校验方法，校验接口是否为spring生命周期接口、内部语言接口、接口是否存在方法
			if (!isConfigurationCallbackInterface(ifc) && !isInternalLanguageInterface(ifc) &&
					ifc.getMethods().length > 0) {
				hasReasonableProxyInterface = true;
				break;
			}
		}
		// 存在有用接口
		if (hasReasonableProxyInterface) {
			// Must allow for introductions; can't just set interfaces to the target's interfaces only.
			// 必须引入，不嫩仅仅设置接口为target的接口
			for (Class<?> ifc : targetInterfaces) {
				proxyFactory.addInterface(ifc);
			}
		}
		else {
			// 没有接口，使用CGLIB
			proxyFactory.setProxyTargetClass(true);
		}
	}

	/**
	 * 判断给定接口是否为容器的回调接口，回调接口不应该被视为合理的代理接口
	 * 包含：lifecycle、aware等接口
	 */
	protected boolean isConfigurationCallbackInterface(Class<?> ifc) {
		return (InitializingBean.class == ifc || DisposableBean.class == ifc || Closeable.class == ifc ||
				AutoCloseable.class == ifc || ObjectUtils.containsElement(ifc.getInterfaces(), Aware.class));
	}

	/**
	 * 判断给定的接口是否是一个众所周知的内部语言接口，因此不应被视为一个合理的代理接口。
	 */
	protected boolean isInternalLanguageInterface(Class<?> ifc) {
		return (ifc.getName().equals("groovy.lang.GroovyObject") ||
				ifc.getName().endsWith(".cglib.proxy.Factory") ||
				ifc.getName().endsWith(".bytebuddy.MockAccess"));
	}

}
