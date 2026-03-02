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

package org.springframework.aop.config;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * 用于处理AOP auto-proxy creator 自动代理创建器的使用工具
 *
 * 虽然只应该注册一个auto-proxy creator，但是存在多个具体实现。
 * 此类提供了一个简单的升级协议，允许调用者请求特定的auto-proxy creator，
 * 确保该creator或功能更强的creator被注册为BeanPostProcess
 */
public abstract class AopConfigUtils {

	/**
	 * The bean name of the internally managed auto-proxy creator.
	 * 内部管理的自动代理创建器的bean名称。
	 */
	public static final String AUTO_PROXY_CREATOR_BEAN_NAME =
			"org.springframework.aop.config.internalAutoProxyCreator";

	/**
	 * Stores the auto proxy creator classes in escalation order.
	 * 以递增顺序存储自动代理创建器类。位置索引越大，优先级越高
	 */
	private static final List<Class<?>> APC_PRIORITY_LIST = new ArrayList<>(3);

	static {
		// Set up the escalation list...
		APC_PRIORITY_LIST.add(InfrastructureAdvisorAutoProxyCreator.class);
		APC_PRIORITY_LIST.add(AspectJAwareAdvisorAutoProxyCreator.class);
		APC_PRIORITY_LIST.add(AnnotationAwareAspectJAutoProxyCreator.class);
	}

	// 注册AutoProxyCreator：默认 InfrastructureAdvisorAutoProxyCreator
	public static @Nullable BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAutoProxyCreatorIfNecessary(registry, null);
	}
	// 注册AutoProxyCreator：InfrastructureAdvisorAutoProxyCreator
	public static @Nullable BeanDefinition registerAutoProxyCreatorIfNecessary(
			BeanDefinitionRegistry registry, @Nullable Object source) {

		return registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);
	}
	// 注册AutoProxyCreator：AspectJAwareAdvisorAutoProxyCreator
	public static @Nullable BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAspectJAutoProxyCreatorIfNecessary(registry, null);
	}
	// 注册AutoProxyCreator：AspectJAwareAdvisorAutoProxyCreator
	public static @Nullable BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(
			BeanDefinitionRegistry registry, @Nullable Object source) {

		return registerOrEscalateApcAsRequired(AspectJAwareAdvisorAutoProxyCreator.class, registry, source);
	}
	// 注册AutoProxyCreator：AnnotationAwareAspectJAutoProxyCreator
	public static @Nullable BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry, null);
	}
	// 注册AutoProxyCreator：AnnotationAwareAspectJAutoProxyCreator
	public static @Nullable BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(
			BeanDefinitionRegistry registry, @Nullable Object source) {

		return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, source);
	}
	// 使用CGLIB代理
	public static void forceAutoProxyCreatorToUseClassProxying(BeanDefinitionRegistry registry) {
		defaultProxyConfig(registry).getPropertyValues().add("proxyTargetClass", Boolean.TRUE);
	}
	// 暴露proxy对象到aop-context
	public static void forceAutoProxyCreatorToExposeProxy(BeanDefinitionRegistry registry) {
		defaultProxyConfig(registry).getPropertyValues().add("exposeProxy", Boolean.TRUE);
	}
	// 获取默认aop-proxy config，没有则创建 ProxyConfig 的bean-definition并注册到registry
	private static BeanDefinition defaultProxyConfig(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(AutoProxyUtils.DEFAULT_PROXY_CONFIG_BEAN_NAME)) {
			// 返回已经注册的bean-definition
			return registry.getBeanDefinition(AutoProxyUtils.DEFAULT_PROXY_CONFIG_BEAN_NAME);
		}
		// 不存在则创建ProxyConfig的bean-definition
		RootBeanDefinition beanDefinition = new RootBeanDefinition(ProxyConfig.class);
		beanDefinition.setSource(AopConfigUtils.class);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 注册到registry
		registry.registerBeanDefinition(AutoProxyUtils.DEFAULT_PROXY_CONFIG_BEAN_NAME, beanDefinition);
		return beanDefinition;
	}
	// 注册或升级升级auto-proxy creator
	private static @Nullable BeanDefinition registerOrEscalateApcAsRequired(
			Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");

		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			// auto-proxy creator已经存在，则判断优先级是否升级bean的class
			BeanDefinition beanDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			if (!cls.getName().equals(beanDefinition.getBeanClassName())) {
				int currentPriority = findPriorityForClass(beanDefinition.getBeanClassName());
				int requiredPriority = findPriorityForClass(cls);
				// 如果目标类的优先级大于当前类，则升级apc类为目标类
				if (currentPriority < requiredPriority) {
					// 升级bean对应的class
					beanDefinition.setBeanClassName(cls.getName());
				}
			}
			return null;
		}
		// 没有存在apc，则创建apc对应的bean-definition
		RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
		beanDefinition.setSource(source);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
		registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
		return beanDefinition;
	}
	// 获取apc class对应的优先级
	private static int findPriorityForClass(Class<?> clazz) {
		return APC_PRIORITY_LIST.indexOf(clazz);
	}
	// 获取apc class对应的优先级
	private static int findPriorityForClass(@Nullable String className) {
		for (int i = 0; i < APC_PRIORITY_LIST.size(); i++) {
			Class<?> clazz = APC_PRIORITY_LIST.get(i);
			if (clazz.getName().equals(className)) {
				return i;
			}
		}
		throw new IllegalArgumentException(
				"Class name [" + className + "] is not a known auto-proxy creator class");
	}

}
