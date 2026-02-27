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

package org.springframework.aop.interceptor;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.PriorityOrdered;

/**
 * 实现：MethodInterceptor、PriorityOrdered、Serializable接口。
 *
 * 该interceptor拦截器将当前的MethodInvocation作为thread-local object线程本地对象暴露出来。
 * 偶尔需要这样做：当某个pointcut（如 AspectJ expression pointcut）需要了解invocation context调用上下文
 *
 * 除非必要，否则不要使用此interceptor。target object目标对象通常不应该了解SpringAop的存在，会因此导致对Spring Api的依赖
 * target object 目标对象应该尽可能的是普通POJO（Plain Old Java Object）。
 *
 * 如果需要使用此拦截器，通常应该作为interceptor chain拦截器链的第一个
 */
@SuppressWarnings("serial")
public final class ExposeInvocationInterceptor implements MethodInterceptor, PriorityOrdered, Serializable {

	/**
	 *  Singleton instance of this class.
	 * 	此类的单例对象
	 */
	public static final ExposeInvocationInterceptor INSTANCE = new ExposeInvocationInterceptor();

	/**
	 * 此类的单例advisor，使用Spring AOP时应该优先使用此实例而不是 INSTANCE，
	 * 可以避免为了包装INSTANCE而创建新的advisor对象
	 */
	public static final Advisor ADVISOR = new DefaultPointcutAdvisor(INSTANCE) {
		@Override
		public String toString() {
			return ExposeInvocationInterceptor.class.getName() +".ADVISOR";
		}
	};

	/**
	 * 暴露MethodInvocation类型的JoinPoint到线程本地
	 */
	private static final ThreadLocal<MethodInvocation> invocation =
			new NamedThreadLocal<>("Current AOP method invocation");


	/**
	 * 返回与当前invocation调用关联的AOP Alliance MethodInvocation对象
	 */
	public static MethodInvocation currentInvocation() throws IllegalStateException {
		// 从ThreadLocal中获取该MI
		MethodInvocation mi = invocation.get();
		if (mi == null) {
			// 不存在则抛出异常
			throw new IllegalStateException(
					"No MethodInvocation found: Check that an AOP invocation is in progress and that the " +
					"ExposeInvocationInterceptor is upfront in the interceptor chain. Specifically, note that " +
					"advices with order HIGHEST_PRECEDENCE will execute before ExposeInvocationInterceptor! " +
					"In addition, ExposeInvocationInterceptor and ExposeInvocationInterceptor.currentInvocation() " +
					"must be invoked from the same thread.");
		}
		return mi;
	}


	/**
	 * 确保只能创建规范实例，即INSTANCE单例对象
	 */
	private ExposeInvocationInterceptor() {
	}

	@Override
	public @Nullable Object invoke(MethodInvocation mi) throws Throwable {
		// 执行前，获取old
		MethodInvocation oldInvocation = invocation.get();
		// 设置当前mi
		invocation.set(mi);
		try {
			// mi执行
			return mi.proceed();
		}
		finally {
			//结束后，还原回old
			invocation.set(oldInvocation);
		}
	}
}
