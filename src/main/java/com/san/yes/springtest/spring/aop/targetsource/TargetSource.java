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

import org.jspecify.annotations.Nullable;

/**
 * TargetSource用于Aop调用时获取当前target对象，
 * 如果没有环绕通知 around advice自行选择结束interceptor chain，则会通过返回调用该target对象
 *
 * 如果target-source是static静态的，它将始终返回相同的对象，这样AOP框架可以进行一些优化
 * 动态目标则支持：pooling池化、hot swapping热交换等功能
 */
public interface TargetSource extends TargetClassAware {

	/**
	 * 返回此 TargetSource 所返回的目标对象的类型。
	 * 可以返回 null，尽管某些 TargetSource 的使用方式可能仅依赖于一个预定义的目标类即可正常工作。
	 */
	@Override
	@Nullable Class<?> getTargetClass();

	/**
	 * 是否所有对 getTarget() 的调用都会返回相同的对象？
	 * 如果是这样，则无需调用 releaseTarget(Object)，并且 AOP 框架可以缓存 getTarget() 的返回值。
	 * 默认实现返回 false。
	 */
	default boolean isStatic() {
		return false;
	}

	/**
	 * 返回一个目标实例。在AOP框架调用AOP方法调用的“目标”之前立即调用此方法。
	 */
	@Nullable Object getTarget() throws Exception;

	/**
	 * 释放从 getTarget() 方法获得的给定目标对象（如果有的话）。
	 */
	default void releaseTarget(Object target) throws Exception {
	}

}
