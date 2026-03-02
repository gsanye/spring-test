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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.autoproxy.TargetSourceCreator;
import org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.util.Assert;

/**
 * 基于BeanFactroy的TargetSource创建器，方便子类创建TargetSource
 *
 * 使用一个内部BeanFactory，管理target实例：将原始的bean-definition复制到内部BeanFactory。
 * 因为原始Beanfactory只包含auto-proxy创建的proxy-instance
 */
public abstract class AbstractBeanFactoryBasedTargetSourceCreator
		implements TargetSourceCreator, BeanFactoryAware, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 原始BeanFactory
	 */
	private @Nullable ConfigurableBeanFactory beanFactory;

	/**
	 * Internally used DefaultListableBeanFactory instances, keyed by bean name.
	 * 内部使用的DefaultListableBeanFactory，key为beanName
	 */
	private final Map<String, DefaultListableBeanFactory> internalBeanFactories = new HashMap<>();


	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableBeanFactory clbf)) {
			throw new IllegalStateException("Cannot do auto-TargetSource creation with a BeanFactory " +
					"that doesn't implement ConfigurableBeanFactory: " + beanFactory.getClass());
		}
		this.beanFactory = clbf;
	}

	/**
	 * Return the BeanFactory that this TargetSourceCreators runs in.
	 * 返回此TargetSourceCreator在其中运行的BeanFactory：原始BeanFactory
	 */
	protected final @Nullable BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	private ConfigurableBeanFactory getConfigurableBeanFactory() {
		Assert.state(this.beanFactory != null, "BeanFactory not set");
		return this.beanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of the TargetSourceCreator interface
	//---------------------------------------------------------------------

	@Override
	public final @Nullable TargetSource getTargetSource(Class<?> beanClass, String beanName) {
		// 创建基于BeanFactory的TargetSource--》模板方法，子类实现
		AbstractBeanFactoryBasedTargetSource targetSource =
				createBeanFactoryBasedTargetSource(beanClass, beanName);
		// 不需要targetSource，则直接返回null
		if (targetSource == null) {
			return null;
		}
		// 获取内部BeanFactory
		DefaultListableBeanFactory internalBeanFactory = getInternalBeanFactoryForBean(beanName);

		// We need to override just this bean definition, as it may reference other beans
		// and we're happy to take the parent's definition for those.
		// Always use prototype scope if demanded.
		// 从原始BeanFactroy中获取bean-definition，复制到内部BeanFactory
		BeanDefinition bd = getConfigurableBeanFactory().getMergedBeanDefinition(beanName);
		GenericBeanDefinition bdCopy = new GenericBeanDefinition(bd);
		if (isPrototypeBased()) {
			bdCopy.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		}
		// 注册到内部BeanFactory
		internalBeanFactory.registerBeanDefinition(beanName, bdCopy);

		// Complete configuring the PrototypeTargetSource.
		// 设置基于BeanFactory的TargetSource的beanName、内部BeanFactory
		targetSource.setTargetBeanName(beanName);
		targetSource.setBeanFactory(internalBeanFactory);

		return targetSource;
	}

	/**
	 * 返回用于指定bean的内部BeanFactory。
	 */
	protected DefaultListableBeanFactory getInternalBeanFactoryForBean(String beanName) {
		synchronized (this.internalBeanFactories) {
			// 先查询缓存，没有则创建一个新的内部BeanFactory
			return this.internalBeanFactories.computeIfAbsent(beanName,
					name -> buildInternalBeanFactory(getConfigurableBeanFactory()));
		}
	}

	/**
	 * Build an internal BeanFactory for resolving target beans.
	 * 构建一个用于解析目标 bean 的内部 BeanFactory: parent为原始BeanFactory
	 */
	protected DefaultListableBeanFactory buildInternalBeanFactory(ConfigurableBeanFactory containingFactory) {
		// Set parent so that references (up container hierarchies) are correctly resolved.
		// 设置原始BeanFactory为父容器，以便正确解析引用（向上解析容器层次结构）。
		DefaultListableBeanFactory internalBeanFactory = new DefaultListableBeanFactory(containingFactory);

		// Required so that all BeanPostProcessors, Scopes, etc become available.
		//这是为了确保所有的 BeanPostProcessor、作用域（Scopes）等都可用。
		internalBeanFactory.copyConfigurationFrom(containingFactory);

		// Filter out BeanPostProcessors that are part of the AOP infrastructure,
		// since those are only meant to apply to beans defined in the original factory.
		// 过滤掉作为AOP基础设施一部分的BeanPostProcessor， 因为这些BeanPostProcessor仅适用于原始工厂中定义的bean。
		internalBeanFactory.getBeanPostProcessors().removeIf(AopInfrastructureBean.class::isInstance);

		return internalBeanFactory;
	}

	/**
	 * Destroys the internal BeanFactory on shutdown of the TargetSourceCreator.
	 * @see #getInternalBeanFactoryForBean
	 */
	@Override
	public void destroy() {
		synchronized (this.internalBeanFactories) {
			for (DefaultListableBeanFactory bf : this.internalBeanFactories.values()) {
				bf.destroySingletons();
			}
		}
	}


	//---------------------------------------------------------------------
	// Template methods to be implemented by subclasses
	//子类需实现的模板方法
	//---------------------------------------------------------------------

	/**
	 * 返回此 TargetSourceCreator 是否基于原型（prototype-based）。目标 bean 定义的作用域将根据此设置。
	 */
	protected boolean isPrototypeBased() {
		return true;
	}

	/**
	 * 子类实现：
	 * 1. 如果希望为此bean创建自定义TargetSource则返回一个新的AbstractBeanFactoryBasedTargetSource
	 * 2. 如果bean不符合，则返回null，不创建TargetSource
	 */
	protected abstract @Nullable AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(
			Class<?> beanClass, String beanName);

}
