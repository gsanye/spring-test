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

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.core.SmartClassLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 实现了BeanPostProcessor接口的类，用于为每个符合条件的bea创建一个AOP proxy，
 * 再调用bean本身方法之前，委托给给定的interceptor
 * 只要TargetSourceCreator指定了自定义TargetSource，例如pooling，即使没有advice，也会自动代理
 */
@SuppressWarnings("serial")
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
		implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

	/**
	 * Convenience constant for subclasses: Return value for "do not proxy".
	 * 方便子类使用的常量："不代理"的返回值
	 */
	protected static final Object @Nullable [] DO_NOT_PROXY = null;

	/**
	 * Convenience constant for subclasses: Return value for
	 * "proxy without additional interceptors, just the common ones".
	 * 方便子类使用的常量：返回“没有额外拦截器，仅包含通用拦截器”的代理对象的值。
	 */
	protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];


	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Default is global AdvisorAdapterRegistry.
	 * 默认：DefaultAdvisorAdapterRegistry
	 * */
	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	/** Default is no common interceptors. */
	private String[] interceptorNames = new String[0];

	private boolean applyCommonInterceptorsFirst = true;

	// 目标源创建器，方法：getTargetSource(beanClass,beanName)
	// 两个实现类：QucikTargetSourceCreator、LazyInitTargetSourceCreator
	private TargetSourceCreator @Nullable [] customTargetSourceCreators;

	private @Nullable BeanFactory beanFactory;

	// 存在targetSosurce的bean集合
	private final Set<String> targetSourcedBeans = ConcurrentHashMap.newKeySet(16);

	// 提前引用bean缓存
	private final Map<Object, Object> earlyBeanReferences = new ConcurrentHashMap<>(16);
	// proxy class 缓存
	private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<>(16);
	// 已经通知缓存
	private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256);


	/**
	 * 指定要使用的AdvisorAdapterRegistry。默认是全局的AdvisorAdapterRegistry。
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	/**
	 * 设置自定义的TargetSourceCreator。
	 * 主意：即使没有任何advice或advisor的target bean，creator也会被调用，
	 * 如果某个creator为bean返回了一个TargetSource，那么必定为该bean创建proxy。
	 */
	public void setCustomTargetSourceCreators(TargetSourceCreator... targetSourceCreators) {
		this.customTargetSourceCreators = targetSourceCreators;
	}

	/**
	 * 设置公共拦截器common interceptors，必须是当前factory中的bean name，
	 * 可以是任何类型的advice或advisor
	 */
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * 设置是否通用拦截器common interceptor优先应用于特定的拦截器。 默认为true
	 */
	public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
		this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		AutoProxyUtils.applyDefaultProxyConfig(this, beanFactory);
	}


	protected @Nullable BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	// getBeanNamesForType()的时候会根据每个BeanName去匹配类型合适的Bean，这里不例外，也会帮忙在proxyTypes找一下
	@Override
	public @Nullable Class<?> predictBeanType(Class<?> beanClass, String beanName) {
		if (this.proxyTypes.isEmpty()) {
			return null;
		}
		Object cacheKey = getCacheKey(beanClass, beanName);
		return this.proxyTypes.get(cacheKey);
	}

	// 确定bean的类型
	@Override
	public Class<?> determineBeanType(Class<?> beanClass, String beanName) {
		Object cacheKey = getCacheKey(beanClass, beanName);
		// 检查缓存
		Class<?> proxyType = this.proxyTypes.get(cacheKey);
		if (proxyType == null) {
			// 获取targetSource
			TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
			if (targetSource != null) {
				if (StringUtils.hasLength(beanName)) {
					// 缓存有targetSource的bean
					this.targetSourcedBeans.add(beanName);
				}
			}
			else {
				//空targetSource
				targetSource = EmptyTargetSource.forClass(beanClass);
			}
			// 获取特定拦截器链
			Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
			if (specificInterceptors != DO_NOT_PROXY) {
				// 存在拦截器链，缓存
				this.advisedBeans.put(cacheKey, Boolean.TRUE);
				// 创建proxyClass
				proxyType = createProxyClass(beanClass, beanName, specificInterceptors, targetSource);
				// 缓存proxyClass
				this.proxyTypes.put(cacheKey, proxyType);
			}
		}
		return (proxyType != null ? proxyType : beanClass);
	}
	// 推理构造器函数，不做处理，返回null
	@Override
	public Constructor<?> @Nullable [] determineCandidateConstructors(Class<?> beanClass, String beanName) {
		return null;
	}

	// getEarlyBeanReference()它是为了解决单例bean之间的循环依赖问题，提前将代理对象暴露出去
	@Override
	public Object getEarlyBeanReference(Object bean, String beanName) {
		Object cacheKey = getCacheKey(bean.getClass(), beanName);
		this.earlyBeanReferences.put(cacheKey, bean);
		return wrapIfNecessary(bean, beanName, cacheKey);
	}

	/**
	 * 实例化前回调，在bean实例化前给proxy一个机会。
	 * 简单来说：如果bean存在自定义TargetSource，则此处会创建proxy（也会获取interceptor-chain）
	 */
	@Override
	public @Nullable Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
		Object cacheKey = getCacheKey(beanClass, beanName);

		if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
			// 已经被通知了，直接返回null
			if (this.advisedBeans.containsKey(cacheKey)) {
				return null;
			}
			// isInfrastructureClass:Advice、Pointcut、Advisor、AopInfrastructureBean的子类，表示是框架所属的Bean
			// shouldSkip:默认都是返回false的。AspectJAwareAdvisorAutoProxyCreator重写此方法：只要存在一个Advisor   ((AspectJPointcutAdvisor) advisor).getAspectName().equals(beanName)成立  就返回true
			if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
				this.advisedBeans.put(cacheKey, Boolean.FALSE);
				return null;
			}
		}

		// 如果存在自定义TargetSource，此处会创建proxy
		// 这会抑制target bean不必要的默认实例化流程：TargetSource将以自定义的方式处理target instance 目标实例
		// getCustomTargetSource逻辑：存在TargetSourceCreator  并且 beanFactory.containsBean(beanName)  然后遍历所有的TargetSourceCreator，调用getTargetSource谁先创建不为null就终止
		TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
		if (targetSource != null) {
			// 存在target-srouce就会创建proxy
			if (StringUtils.hasLength(beanName)) {
				this.targetSourcedBeans.add(beanName);
			}
			//getAdvicesAndAdvisorsForBean：方法判断当前bean是否需要进行代理，若需要则返回满足条件的Advice或者Advisor集合
			// 这个方法由子类实现，AbstractAdvisorAutoProxyCreator和BeanNameAutoProxyCreator  代表中两种不同的代理方式
			Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
			// 根据目标对象创建代理实对象实例
			Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
			// 缓存proxy class
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		return null;
	}
	// 属性配置，默认不处理
	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
		return pvs;  // skip postProcessPropertyValues
	}

	/**
	 * 实例化后回调：判断是否需要代理，已经提前应用代理过的则会跳过
	 */
	@Override
	public @Nullable Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
		if (bean != null) {
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			// 提前引用时已经代理
			if (this.earlyBeanReferences.remove(cacheKey) != bean) {
				// 判断是否需要代理
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}


	/**
	 * 为给定的 bean 类和 bean 名称构建缓存键。
	 */
	protected Object getCacheKey(Class<?> beanClass, @Nullable String beanName) {
		if (StringUtils.hasLength(beanName)) {
			return new ComposedCacheKey(beanClass, beanName);
		}
		else {
			return beanClass;
		}
	}

	/**
	 * 如果符合代理条件，则对给定bean进行代理包装
	 */
	protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		// targetSource中存在，则说明已经代理过了
		// postProcessBeforeInstantiation()中如果有自定义的TargetSource则已经创建过proxy
		if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
			return bean;
		}
		// 如果bean为基础框架bean或免代理bean，则不处理
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}
		// 如果bean为基础框架bean或免代理bean，则不处理
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			// 缓存
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

		// 如果有advice则创建代理
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		if (specificInterceptors != DO_NOT_PROXY) {
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}
		// 不需要代理，也把这种不需要代理的对象给与缓存起来  赋值为false
		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}

	/**
	 * 返回给定bean是否为基础bean
	 */
	protected boolean isInfrastructureClass(Class<?> beanClass) {
		boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
				Pointcut.class.isAssignableFrom(beanClass) ||
				Advisor.class.isAssignableFrom(beanClass) ||
				AopInfrastructureBean.class.isAssignableFrom(beanClass);
		if (retVal && logger.isTraceEnabled()) {
			logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
		}
		return retVal;
	}

	/**
	 * 子类应重写此方法，如果给定的 bean 不应被此后置处理器考虑进行自动代理，则返回 true。
	 * 有时我们需要能够避免这种情况发生，例如，如果这会导致循环引用，或者如果需要保留现有的目标实例。
	 * 除非 bean 名称根据 AutowireCapableBeanFactory 的约定指示这是一个“原始实例”，否则此实现将返回 false。
	 */
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		return AutoProxyUtils.isOriginalInstance(beanName, beanClass);
	}

	/**
	 * 为 bean 实例创建一个目标源。如果设置了 TargetSourceCreators，则使用这些创建器。
	 * 如果不应使用自定义的 TargetSource，则返回 null。
	 */
	protected @Nullable TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
		// We can't create fancy target sources for directly registered singletons.
		if (this.customTargetSourceCreators != null &&
				this.beanFactory != null && this.beanFactory.containsBean(beanName)) {
			for (TargetSourceCreator tsc : this.customTargetSourceCreators) {
				TargetSource ts = tsc.getTargetSource(beanClass, beanName);
				if (ts != null) {
					// Found a matching TargetSource.
					if (logger.isTraceEnabled()) {
						logger.trace("TargetSourceCreator [" + tsc +
								"] found custom TargetSource for bean with name '" + beanName + "'");
					}
					// 第一个返回的TargetSource
					return ts;
				}
			}
		}

		// No custom TargetSource found.
		return null;
	}

	/**
	 * 为给定的 bean 创建一个 AOP 代理。
	 */
	protected Object createProxy(Class<?> beanClass, @Nullable String beanName,
			Object @Nullable [] specificInterceptors, TargetSource targetSource) {

		return buildProxy(beanClass, beanName, specificInterceptors, targetSource, false);
	}
	// 为给定的 bean 创建一个 AOP 代理类class。
	private Class<?> createProxyClass(Class<?> beanClass, @Nullable String beanName,
			Object @Nullable [] specificInterceptors, TargetSource targetSource) {

		return (Class<?>) buildProxy(beanClass, beanName, specificInterceptors, targetSource, true);
	}

	/**
	 * 创建代理对象
	 * specificInterceptors：拦截器链，作用于bean上的advice和advisor
	 * targetSource：此处传入的为TargetSource，并非直接的target，所以intercptor每次处理时需要调用TargetSource
	 */
	private Object buildProxy(Class<?> beanClass, @Nullable String beanName,
			Object @Nullable [] specificInterceptors, TargetSource targetSource, boolean classOnly) {

		if (this.beanFactory instanceof ConfigurableListableBeanFactory clbf) {
			// 设置factory中bean-definition，记录bean的原始class
			AutoProxyUtils.exposeTargetClass(clbf, beanName, beanClass);
		}
		// 使用ProxyFactory创建AOP proxy
		ProxyFactory proxyFactory = new ProxyFactory();
		// 从当前类复制相关AOP配置，以为当前类也继承了ProxyConfig
		proxyFactory.copyFrom(this);
		proxyFactory.setFrozen(false);

		// 判断是否需要基于类的代理（CGLIB），需要进一步判断
		if (shouldProxyTargetClass(beanClass, beanName)) {
			proxyFactory.setProxyTargetClass(true);
		}
		else {
			// 是否存在要暴露的接口
			Class<?>[] ifcs = (this.beanFactory instanceof ConfigurableListableBeanFactory clbf ?
					AutoProxyUtils.determineExposedInterfaces(clbf, beanName) : null);
			if (ifcs != null) {
				// 存在暴露接口，使用jdk代理
				proxyFactory.setProxyTargetClass(false);
				for (Class<?> ifc : ifcs) {
					// 添加接口
					proxyFactory.addInterface(ifc);
				}
			}
			// 如果不存在暴露接口或proxyFactory非类代理
			if (ifcs != null ? ifcs.length == 0 : !proxyFactory.isProxyTargetClass()) {
				// 添加类接口到proxyFactory
				evaluateProxyInterfaces(beanClass, proxyFactory);
			}
		}

		if (proxyFactory.isProxyTargetClass()) {
			// Explicit handling of JDK proxy targets and lambdas (for introduction advice scenarios)
			// 显式处理JDK代理目标和lambda表达式（用于引介建议场景）
			if (Proxy.isProxyClass(beanClass) || ClassUtils.isLambdaClass(beanClass)) {
				// Must allow for introductions; can't just set interfaces to the proxy's interfaces only.
				for (Class<?> ifc : beanClass.getInterfaces()) {
					proxyFactory.addInterface(ifc);
				}
			}
		}
		// 合并最终的advisors：特定的拦截器+公共拦截器，顺序由参数applyCommonInterceptorsFirst决定
		Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
		// 加强通知拦截器添加到proxyFactory
		proxyFactory.addAdvisors(advisors);
		// 设置targetSource
		proxyFactory.setTargetSource(targetSource);
		// 子类可重写，自定义proxyFactory
		customizeProxyFactory(proxyFactory);
		// 沿用this的freezeProxy的属性值
		proxyFactory.setFrozen(isFrozen());
		// 设置preFiltered的属性值，默认是false。子类：AbstractAdvisorAutoProxyCreator修改为true
		// preFiltered：字段意思为：是否已为特定目标类筛选Advisor
		// 这个字段和DefaultAdvisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice获取所有的Advisor有关
		// CglibAopProxy和JdkDynamicAopProxy都会调用此方法，然后递归执行所有的Advisor的
		if (advisorsPreFiltered()) {
			proxyFactory.setPreFiltered(true);
		}

		// //如果bean类未在覆盖类加载器中本地加载，则使用原始的ClassLoader。
		ClassLoader classLoader = getProxyClassLoader();
		if (classLoader instanceof SmartClassLoader smartClassLoader && classLoader != beanClass.getClassLoader()) {
			classLoader = smartClassLoader.getOriginalClassLoader();
		}
		// 根据参数获取proxy实例或class对象
		return (classOnly ? proxyFactory.getProxyClass(classLoader) : proxyFactory.getProxy(classLoader));
	}

	/**
	 * 判断给定的 bean 是否应该使用其目标类（而非接口）进行代理。
	 * 检查相应 bean 定义中的 "preserveTargetClass" 属性。
	 */
	protected boolean shouldProxyTargetClass(Class<?> beanClass, @Nullable String beanName) {
		return (this.beanFactory instanceof ConfigurableListableBeanFactory clbf &&
				AutoProxyUtils.shouldProxyTargetClass(clbf, beanName));
	}

	/**
	 * 返回子类返回的 Advisor 是否已经预先过滤以匹配 Bean 的目标类，
	 * 从而允许在构建用于 AOP 调用的 Advisor 链时跳过 ClassFilter 的检查。
	 * 默认值为 false。如果子类始终返回已预先过滤的 Advisor，则可覆盖此方法。
	 */
	protected boolean advisorsPreFiltered() {
		return false;
	}

	/**
	 * 为给定的 bean 确定 Advisor，包括特定的拦截器以及公共的拦截器，所有这些拦截器都适配到 Advisor 接口。
	 */
	protected Advisor[] buildAdvisors(@Nullable String beanName, Object @Nullable [] specificInterceptors) {
		// Handle prototypes correctly...
		Advisor[] commonInterceptors = resolveInterceptorNames();

		List<Object> allInterceptors = new ArrayList<>();
		if (specificInterceptors != null) {
			if (specificInterceptors.length > 0) {
				// specificInterceptors may equal PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
				allInterceptors.addAll(Arrays.asList(specificInterceptors));
			}
			if (commonInterceptors.length > 0) {
				if (this.applyCommonInterceptorsFirst) {
					allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
				}
				else {
					allInterceptors.addAll(Arrays.asList(commonInterceptors));
				}
			}
		}
		if (logger.isTraceEnabled()) {
			int nrOfCommonInterceptors = commonInterceptors.length;
			int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
			logger.trace("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
					" common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
		}

		Advisor[] advisors = new Advisor[allInterceptors.size()];
		for (int i = 0; i < allInterceptors.size(); i++) {
			advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
		}
		return advisors;
	}

	/**
	 * Resolves the specified interceptor names to Advisor objects.
	 * 将指定的拦截器名称解析为Advisor对象
	 */
	private Advisor[] resolveInterceptorNames() {
		BeanFactory bf = this.beanFactory;
		ConfigurableBeanFactory cbf = (bf instanceof ConfigurableBeanFactory _cbf ? _cbf : null);
		List<Advisor> advisors = new ArrayList<>();
		for (String beanName : this.interceptorNames) {
			if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
				Assert.state(bf != null, "BeanFactory required for resolving interceptor names");
				Object next = bf.getBean(beanName);
				advisors.add(this.advisorAdapterRegistry.wrap(next));
			}
		}
		return advisors.toArray(new Advisor[0]);
	}

	/**
	 * 子类可以选择实现此方法：例如，用于更改暴露的接口。
	 * 默认实现为空。
	 */
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}


	/**
	 * 返回是否应为给定的 bean 创建代理，以及应应用哪些额外的advice（例如 AOP Alliance interceptor）和advisor。
	 */
	protected abstract Object @Nullable [] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
			@Nullable TargetSource customTargetSource) throws BeansException;


	/**
	 * Composed cache key for bean class plus bean name.
	 * 为 Bean 类加上 Bean 名称组合而成的缓存键。
	 */
	private record ComposedCacheKey(Class<?> beanClass, String beanName) {
	}

}
