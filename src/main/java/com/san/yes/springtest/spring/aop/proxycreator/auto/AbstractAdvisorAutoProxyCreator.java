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

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

/**
 * 通用的auto-proxy creator。能够根据检测到每个bean的advisor为bean构建aop proxy
 *
 * 子类也可以覆盖
 * 1. findCandidateAdvisors方法：用于返回任意对象的自定义advisor列表
 * 2. shouldSkip方法：用于排除某些对象不被auto-proxy
 *
 * 排序：按照@Order注解或Ordered接口的顺序排序，未实现Ordered接口和@Order注解的按照顺序在advisor chain的末尾
 */
@SuppressWarnings("serial")
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {

	// 重点类：advisor检测助手
	private @Nullable BeanFactoryAdvisorRetrievalHelper advisorRetrievalHelper;

	// 重写setBeanFactory方法，用户初始化advisor检测器
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		if (!(beanFactory instanceof ConfigurableListableBeanFactory clbf)) {
			throw new IllegalArgumentException(
					"AdvisorAutoProxyCreator requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		// 初始化advisorRetrievalHelper
		initBeanFactory(clbf);
	}

	protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// 默认实现为内部类
		this.advisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);
	}

	// 获取应用于bean的切面。最终委托给BeanFactoryAdvisorRetrievalHelper完成
	@Override
	protected Object @Nullable [] getAdvicesAndAdvisorsForBean(
			Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {
		// 寻找合适的切面
		List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
		if (advisors.isEmpty()) {
			return DO_NOT_PROXY;
		}
		return advisors.toArray();
	}

	/**
	 * 查找所有适用于此类auto-proxy的advisor
	 */
	protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
		// 查找处所有候选advisor
		List<Advisor> candidateAdvisors = findCandidateAdvisors();
		// 过滤上述advisor，查看advisor是否可以应用到该bean，根据advisor的pointcut进行匹配
		// 委托给AopUtils.findAdvisorThatCanApply方法，逻辑为：ClassFilter、MethodMatcher等匹配，有有一个能匹配上就可以
		// 获取方法：ReflectionUtils.getAllDeclaredMethods 获取所有方法，包括私有方法，因此私有方法也会参与匹配。
		List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
		// hook方法，子类可以对符合条件的advisor进行增删改等操作。
		// AspectJAwareAdvisorAutoProxyCreator提供了实现
		extendAdvisors(eligibleAdvisors);
		// 存在符合条件的advisor
		if (!eligibleAdvisors.isEmpty()) {
			try {
				// 对符合条件advisor进行排序：order接口相关
				// AspectjAwareAdvisorAutoProxyCreator复写了此方法
				eligibleAdvisors = sortAdvisors(eligibleAdvisors);
			}
			catch (BeanCreationException ex) {
				throw new AopConfigException("Advisor sorting failed with unexpected bean creation, probably due " +
						"to custom use of the Ordered interface. Consider using the @Order annotation instead.", ex);
			}
		}
		// 返回排序后的符合条件advisor
		return eligibleAdvisors;
	}

	/**
	 * 查找所有可用于auto-proxy的advisor
	 */
	protected List<Advisor> findCandidateAdvisors() {
		Assert.state(this.advisorRetrievalHelper != null, "No BeanFactoryAdvisorRetrievalHelper available");
		// 委托给advisorRetrievalHelper检测助手完成
		return this.advisorRetrievalHelper.findAdvisorBeans();
	}

	/**
	 * 从给定候选的advisor中查找可以应用于指定bean的advisor
	 */
	protected List<Advisor> findAdvisorsThatCanApply(
			List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {
		//当前bean proxy创建中
		ProxyCreationContext.setCurrentProxiedBeanName(beanName);
		try {
			// 委托给AopUtils.findAdvisorsThatCanApply方法过滤适合bean的advisor
			return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
		}
		finally {
			ProxyCreationContext.setCurrentProxiedBeanName(null);
		}
	}

	/**
	 * 返回给定name的advisor bean是否适合首先被代理
	 * Return whether the Advisor bean with the given name is eligible
	 * for proxying in the first place.
	 */
	protected boolean isEligibleAdvisorBean(String beanName) {
		return true;
	}

	/**
	 * 基于order对advisor进行排序，子类可以选择重写此方法实现自定义排序策略
	 */
	protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
		AnnotationAwareOrderComparator.sort(advisors);
		return advisors;
	}

	/**
	 * 扩展hook方法，子类可以重写此方法，额外注册advisor
	 * 默认实现为空，通常用于添加一些advisor，用于暴露后续某些advisor需要的context信息
	 */
	protected void extendAdvisors(List<Advisor> candidateAdvisors) {
	}

	/**
	 * This auto-proxy creator always returns pre-filtered Advisors.
	 * auto-proxy creator总是返回true，是否已经提前过滤advisor
	 */
	@Override
	protected boolean advisorsPreFiltered() {
		return true;
	}


	/**
	 * Subclass of BeanFactoryAdvisorRetrievalHelper that delegates to
	 * surrounding AbstractAdvisorAutoProxyCreator facilities.
	 */
	private class BeanFactoryAdvisorRetrievalHelperAdapter extends BeanFactoryAdvisorRetrievalHelper {

		public BeanFactoryAdvisorRetrievalHelperAdapter(ConfigurableListableBeanFactory beanFactory) {
			super(beanFactory);
		}

		@Override
		protected boolean isEligibleBean(String beanName) {
			// 重写此方法
			return AbstractAdvisorAutoProxyCreator.this.isEligibleAdvisorBean(beanName);
		}
	}

}
