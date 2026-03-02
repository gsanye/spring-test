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

package org.springframework.aop.target;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.TargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 这是一个基于 Spring BeanFactory 的 TargetSource 实现的基类，委托 Spring 管理的 Bean 实例进行操作。
 * 子类可以创建原型实例（prototype instances），或者延迟访问单例目标（singleton target）。
 */
public abstract class AbstractBeanFactoryBasedTargetSource implements TargetSource, BeanFactoryAware, Serializable {

	/**
	 * Name of the target bean we will create on each invocation.
	 * 每次调用时我们将创建的目标 bean 的名称。
	 * */
	protected @Nullable String targetBeanName;

	/** Class of the target. */
	private volatile @Nullable Class<?> targetClass;

	/**
	 * 拥有此目标源的 BeanFactory。我们需要保留此引用，以便在必要时能够创建新的原型实例。
	 */
	@SuppressWarnings("serial")
	private @Nullable BeanFactory beanFactory;


	/**
	 * Set the name of the target bean in the factory.
	 * <p>The target bean should not be a singleton, else the same instance will
	 * always be obtained from the factory, resulting in the same behavior as
	 * provided by {@link SingletonTargetSource}.
	 * @param targetBeanName name of the target bean in the BeanFactory
	 * that owns this interceptor
	 * @see SingletonTargetSource
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	/**
	 * Return the name of the target bean in the factory.
	 */
	public String getTargetBeanName() {
		Assert.state(this.targetBeanName != null, "Target bean name not set");
		return this.targetBeanName;
	}

	/**
	 * Specify the target class explicitly, to avoid any kind of access to the
	 * target bean (for example, to avoid initialization of a FactoryBean instance).
	 * <p>Default is to detect the type automatically, through a {@code getType}
	 * call on the BeanFactory (or even a full {@code getBean} call as fallback).
	 */
	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	/**
	 * Set the owning BeanFactory. We need to save a reference so that we can
	 * use the {@code getBean} method on every invocation.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.targetBeanName == null) {
			throw new IllegalStateException("Property 'targetBeanName' is required");
		}
		this.beanFactory = beanFactory;
	}

	/**
	 * Return the owning BeanFactory.
	 */
	public BeanFactory getBeanFactory() {
		Assert.state(this.beanFactory != null, "BeanFactory not set");
		return this.beanFactory;
	}

	// 获取target的class
	@Override
	public @Nullable Class<?> getTargetClass() {
		Class<?> targetClass = this.targetClass;
		if (targetClass != null) {
			return targetClass;
		}
		synchronized (this) {
			// Full check within synchronization, entering the BeanFactory interaction algorithm only once...
			targetClass = this.targetClass;
			if (targetClass == null && this.beanFactory != null && this.targetBeanName != null) {
				// Determine type of the target bean.
				// 确定目标bean的类型。
				targetClass = this.beanFactory.getType(this.targetBeanName);
				if (targetClass == null) {
					if (logger.isTraceEnabled()) {
						logger.trace("Getting bean with name '" + this.targetBeanName + "' for type determination");
					}
					//
					Object beanInstance = this.beanFactory.getBean(this.targetBeanName);
					targetClass = beanInstance.getClass();
				}
				this.targetClass = targetClass;
			}
			return targetClass;
		}
	}


	/**
	 * Copy configuration from the other AbstractBeanFactoryBasedTargetSource object.
	 * Subclasses should override this if they wish to expose it.
	 * @param other object to copy configuration from
	 */
	protected void copyFrom(AbstractBeanFactoryBasedTargetSource other) {
		this.targetBeanName = other.targetBeanName;
		this.targetClass = other.targetClass;
		this.beanFactory = other.beanFactory;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		AbstractBeanFactoryBasedTargetSource otherTargetSource = (AbstractBeanFactoryBasedTargetSource) other;
		return (ObjectUtils.nullSafeEquals(this.beanFactory, otherTargetSource.beanFactory) &&
				ObjectUtils.nullSafeEquals(this.targetBeanName, otherTargetSource.targetBeanName));
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClass(), this.targetBeanName);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(" for target bean '").append(this.targetBeanName).append('\'');
		Class<?> targetClass = this.targetClass;
		if (targetClass != null) {
			sb.append(" of type [").append(targetClass.getName()).append(']');
		}
		return sb.toString();
	}

}
