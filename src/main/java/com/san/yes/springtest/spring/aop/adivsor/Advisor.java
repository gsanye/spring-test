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

import org.aopalliance.aop.Advice;

/**
 * 基础接口：持有 advice(即在joinpoint处执行的操作)以及一个用于确定adive适用性的过滤器（如pointcut）
 * 此接口并非提供Spring 用户直接使用，而是为了支持不同类型的advice而实现的一种通用性接口
 *
 * Spring AOP基于method interception方法拦截传递的around advice环绕通知。
 * 并且符合AOP Alliance的interception api 拦截api。
 * advisor接口允许支持不同类型的advice，例如：before、after advice，这些advice并不一定必须通过interception实现
 */
public interface Advisor {

	/**
	 * 如果尚未配置合适的advice，则返回空的advice
	 */
	Advice EMPTY_ADVICE = new Advice() {};


	/**
	 * 返回此aspect切面的advice通知部分。
	 * advice可以是：一个interceptor、before advice、throws advice 等
	 */
	Advice getAdvice();

	/**
	 * 返回此advice是否与特定的实例相关联（例如：创建mixmi），
	 * 或是否与同一个Spring Bean-Factory中获取的adviceed类所有实例共享
	 * 方法暂未使用。
	 */
	default boolean isPerInstance() {
		return true;
	}

}
