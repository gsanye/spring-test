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

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.TargetSource;

/**
 * 该接口应由实现AOP代理工厂的配置类实现。
 * 此配置包括：拦截器 interceptor、其他通知 advice、顾问 advisor以及 proxied被代理的接口
 *
 * 任何从Spring中获取的AOP proxy都可以强制转换为该接口，以便能够操作其AOP advice
 */
public interface Advised extends TargetClassAware {

	/**
	 * 返回 Advised 配置是否已冻结，如果是冻结状态，则无法再更改任何通知（advice）。
	 */
	boolean isFrozen();

	/**
	 * 我们是否正在代理整个目标类，而不是指定的接口？
	 */
	boolean isProxyTargetClass();

	/**
	 * 返回由AOP代理所代理的接口。
	 * 不会包含目标类，因为目标类也可能被代理。
	 */
	Class<?>[] getProxiedInterfaces();

	/**
	 * 判断给定的接口是否已被代理。
	 */
	boolean isInterfaceProxied(Class<?> ifc);

	/**
	 * 更改此 Advised 对象所使用的 TargetSource。
	 * 只有在配置未冻结的情况下才能生效。
	 */
	void setTargetSource(TargetSource targetSource);

	/**
	 * 返回此 Advised 对象所使用的 TargetSource。
	 */
	TargetSource getTargetSource();

	/**
	 * 设置是否应由AOP框架将代理对象作为ThreadLocal暴露出来，以便通过AopContext类进行检索。
	 * 如果被通知的对象需要调用自身应用了通知的方法，则可能需要暴露该代理对象。否则，如果被通知的对象调用此方法，则不会应用任何通知。
	 * 默认值为false，以获得最佳性能。
	 */
	void setExposeProxy(boolean exposeProxy);

	/**
	 * 返回工厂是否应将代理对象暴露为 ThreadLocal。
	 * 如果被通知的对象需要调用自身应用了通知的方法，那么暴露代理对象可能是必要的。否则，如果被通知的对象调用此方法，则不会应用任何通知。
	 * 获取代理对象类似于 EJB 调用 getEJBObject()。
	 */
	boolean isExposeProxy();

	/**
	 * 设置此代理配置是否已预先过滤，从而使其仅包含适用的通知（匹配此代理的目标类）。
	 * 默认值为 "false"。如果通知已经过预先过滤（即在构建代理调用的实际通知链时，可以跳过 ClassFilter 检查），则将此值设置为 "true"。
	 */
	void setPreFiltered(boolean preFiltered);

	/**
	 * 返回此代理配置是否已预先过滤，即是否只包含适用的通知（匹配此代理的目标类）。
	 */
	boolean isPreFiltered();

	/**
	 * 返回应用于此代理的advisor顾问。
	 */
	Advisor[] getAdvisors();

	/**
	 * 返回应用于此代理的顾问（advisor）的数量。
	 * 默认实现委托给 getAdvisors().length。
	 */
	default int getAdvisorCount() {
		return getAdvisors().length;
	}

	/**
	 * 在顾问链的末尾添加一个顾问。
	 * 该顾问可以是 `org.springframework.aop.IntroductionAdvisor`，在此情况下，当从相关工厂下次获取代理时，将会有新的接口可用。
	 */
	void addAdvisor(Advisor advisor) throws AopConfigException;

	/**
	 * 在链中的指定位置添加一个 Advisor。
	 * 参数：
	 * pos – 链中的位置（0 表示头部）。必须是有效的
	 */
	void addAdvisor(int pos, Advisor advisor) throws AopConfigException;

	/**
	 * 移除给定的顾问。
	 */
	boolean removeAdvisor(Advisor advisor);

	/**
	 * 移除指定索引处的顾问。
	 */
	void removeAdvisor(int index) throws AopConfigException;

	/**
	 * 返回给定顾问（advisor）的索引（从0开始），如果没有这样的顾问适用于此代理，则返回-1。
	 * 此方法的返回值可用于索引顾问数组。
	 */
	int indexOf(Advisor advisor);

	/**
	 * 替换给定的顾问。
	 * 注意：如果顾问是 `org.springframework.aop.IntroductionAdvisor` 类型，而替换的顾问不是该类或实现了不同的接口，则代理对象需要重新获取，否则旧的接口将不再被支持，而新的接口也不会被实现。
	 * 参数：
	 * a – 要被替换的顾问
	 * b – 用于替换的顾问
	 */
	boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException;

	/**
	 * Add the given AOP Alliance advice to the tail of the advice (interceptor) chain.
	 * <p>This will be wrapped in a DefaultPointcutAdvisor with a pointcut that always
	 * applies, and returned from the {@code getAdvisors()} method in this wrapped form.
	 * <p>Note that the given advice will apply to all invocations on the proxy,
	 * even to the {@code toString()} method! Use appropriate advice implementations
	 * or specify appropriate pointcuts to apply to a narrower set of methods.
	 * @param advice the advice to add to the tail of the chain
	 * @throws AopConfigException in case of invalid advice
	 * @see #addAdvice(int, Advice)
	 * @see org.springframework.aop.support.DefaultPointcutAdvisor
	 */
	void addAdvice(Advice advice) throws AopConfigException;

	/**
	 * Add the given AOP Alliance Advice at the specified position in the advice chain.
	 * <p>This will be wrapped in a {@link org.springframework.aop.support.DefaultPointcutAdvisor}
	 * with a pointcut that always applies, and returned from the {@link #getAdvisors()}
	 * method in this wrapped form.
	 * <p>Note: The given advice will apply to all invocations on the proxy,
	 * even to the {@code toString()} method! Use appropriate advice implementations
	 * or specify appropriate pointcuts to apply to a narrower set of methods.
	 * @param pos index from 0 (head)
	 * @param advice the advice to add at the specified position in the advice chain
	 * @throws AopConfigException in case of invalid advice
	 */
	void addAdvice(int pos, Advice advice) throws AopConfigException;

	/**
	 * Remove the Advisor containing the given advice.
	 * @param advice the advice to remove
	 * @return {@code true} of the advice was found and removed;
	 * {@code false} if there was no such advice
	 */
	boolean removeAdvice(Advice advice);

	/**
	 * Return the index (from 0) of the given AOP Alliance Advice,
	 * or -1 if no such advice is an advice for this proxy.
	 * <p>The return value of this method can be used to index into
	 * the advisors array.
	 * @param advice the AOP Alliance advice to search for
	 * @return index from 0 of this advice, or -1 if there's no such advice
	 */
	int indexOf(Advice advice);

	/**
	 * As {@code toString()} will normally be delegated to the target,
	 * this returns the equivalent for the AOP proxy.
	 * @return a string description of the proxy configuration
	 */
	String toProxyConfigString();

}
