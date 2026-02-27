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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.Advisor;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;

/**
 * 一种简单但明确的方法，用于根据Advised对象，计算出某个方法advice-chain 通知连。
 * 每次都会重新构建每个advice-chain通知链；子类可以提供缓存功能
 */
@SuppressWarnings("serial")
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {

	/**
	 * 此类的单例实例。
	 */
	public static final DefaultAdvisorChainFactory INSTANCE = new DefaultAdvisorChainFactory();


	/**
	 * 确定为给定的advisor-chain configuration顾问链配置生成的一组 MethodInterceptor 对象。
	 */
	@Override
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
			Advised config, Method method, @Nullable Class<?> targetClass) {

		// This is somewhat tricky... We have to process introductions first,
		// but we need to preserve order in the ultimate list.
		// 先处理introduction，但是最终的list顺序保持不变
		// AdvisorAdapterRegistry的作用是将[advisor]转换为[interceptor]，DefaultAdvisorAdapterRegistry则是适配器的默认实现
		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
		// 获取advisor数组
		Advisor[] advisors = config.getAdvisors();
		List<Object> interceptorList = new ArrayList<>(advisors.length);
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
		// 判断是否有IntroductionAdvisor匹配到
		Boolean hasIntroductions = null;

		for (Advisor advisor : advisors) {
			// advisor是PoincutAdvisor的子类
			if (advisor instanceof PointcutAdvisor pointcutAdvisor) {
				// Add it conditionally.
				// 判断[Advisor]是否匹配此targetClass（类级别匹配）
				if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
					// 获取Advisor#Pointcut#MethodMatcher 即方法匹配器
					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
					// Method匹配结果，方法级别
					boolean match;
					if (mm instanceof IntroductionAwareMethodMatcher iamm) {
						if (hasIntroductions == null) {
							hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
						}
						match = iamm.matches(method, actualClass, hasIntroductions);
					}
					else {
						match = mm.matches(method, actualClass);
					}
					if (match) {
						// 通过adapter适配器，将advisor包装成interceptor。为什么是数组？
						// 可能同时实现了前置通知[MethodBeforeAdvice], 后置通知[AfterReturingAdvice], 异常通知接口[ThrowsAdvice]
						// 环绕通知 [MethodInterceptor], 这里会将每个通知统一包装成 MethodInterceptor
						MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
						if (mm.isRuntime()) {
							// Creating a new object instance in the getInterceptors() method
							// isn't a problem as we normally cache created chains.
                            // 如果是运行时动态拦截方法的执行，则创建以恶简单的对象封装相关信息
                            // 它将延迟到方法执行时验证是否要执行此拦截
							for (MethodInterceptor interceptor : interceptors) {
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
							}
						}
						else {
                            // 将结果加入拦截器链
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
				}
			}
            // 判断是否为IntroductionAdvisor
			else if (advisor instanceof IntroductionAdvisor ia) {
                // 判断IntroductionAdvisor引入切面是否适用于目标类，Spring中默认的引入切面是DefaultIntroductionAdvisor类
                // 默认的引入通知是DefaultIntroductionInterceptor，它实现了MethodInterceptor接口
				if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
					Interceptor[] interceptors = registry.getInterceptors(advisor);
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			}
			else {
				Interceptor[] interceptors = registry.getInterceptors(advisor);
				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}

		return interceptorList;
	}

	/**
     * 确定Advisors是否包含匹配的引入通知
	 */
	private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor ia && ia.getClassFilter().matches(actualClass)) {
				return true;
			}
		}
		return false;
	}

}
