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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.AopInvocationException;
import org.springframework.aop.RawTargetAccess;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Spring AOP框架基于JDK dynamic proxy 动态代理的AopProxy实现类
 *
 * 该类创建动态代理，实现AopProxy暴露的接口。动态代理不能用于定义在class中的method，只能用于interface中的method
 *
 * 此类类型的对象应该通过proxy-factory（由AdisedSupport类配置）获取，
 * 此类是Spring AOP框架的内部实现，客户端代码无需直接使用。
 *
 * 如果底层（target）类是线程安全的，则此类创建出来的代理也是线程安全的
 *
 * 如果所有Advisor（包括Advice和Pointcut）以及targetSources都是可序列化的，则代理对象也是可序列化的
 *
 * 我们发现它自己就实现了了InvocationHandler，所以处理器就是它自己。会实现invoke方法
 * 它还是个final类  默认是包的访问权限
 */
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability. */
	private static final long serialVersionUID = 5531744639992436476L;


	private static final String COROUTINES_FLOW_CLASS_NAME = "kotlinx.coroutines.flow.Flow";

	private static final boolean COROUTINES_REACTOR_PRESENT = ClassUtils.isPresent(
			"kotlinx.coroutines.reactor.MonoKt", JdkDynamicAopProxy.class.getClassLoader());

	/** 使用静态log是为了避免序列化问题. */
	private static final Log logger = LogFactory.getLog(JdkDynamicAopProxy.class);

	/** 用于配置此代理的配置信息. */
	private final AdvisedSupport advised;

	/** Cached in {@link AdvisedSupport#proxyMetadataCache}. */
	private transient ProxiedInterfacesCache cache;


	/**
	 * 为给定AOP configuration 构造一个新的JdkDynamicProxy
	 */
	public JdkDynamicAopProxy(AdvisedSupport config) throws AopConfigException {
		Assert.notNull(config, "AdvisedSupport must not be null");
		this.advised = config;

		// Initialize ProxiedInterfacesCache if not cached already
		// 如果cache尚未初缓存，则初始化ProxiedInterfacesCache
		ProxiedInterfacesCache cache;
		if (config.proxyMetadataCache instanceof ProxiedInterfacesCache proxiedInterfacesCache) {
			cache = proxiedInterfacesCache;
		}
		else {
			cache = new ProxiedInterfacesCache(config);
			config.proxyMetadataCache = cache;
		}
		this.cache = cache;
	}


	@Override
	public Object getProxy() {
		return getProxy(ClassUtils.getDefaultClassLoader());
	}

	@Override
	public Object getProxy(@Nullable ClassLoader classLoader) {
		if (logger.isTraceEnabled()) {
			logger.trace("Creating JDK dynamic proxy: " + this.advised.getTargetSource());
		}
		// 这部为重要步骤，根据最终代理的接口（除了自己的接口，还有Spring默认的一些接口）
		// 1. 获取目标对象自己实现的接口们（最终会被代理的接口）
		// 2. 是否添加SpringProxy接口：标记接口，没有添加过则添加
		// 3. 是否添加Adviced接口：注意不是Advise通知接口，没有实现过 且 advised.isOpaque()为false（默认为false）
		// 4. 是否添加DecoratingProxy接口：没有添加过 且 decoratingProxy 为true，则添加接口，主要功能为获取原始代理类类型
		// 5. 代理类的接口一共为：目标对象的接口+3个接口：SpringProxy、Advised、DecoratingProxy

		// 注意，此处传入的InvocationHandler为this，即JdkDynamicAopProxy类，因此增强方法主要看invoke方法
		return Proxy.newProxyInstance(determineClassLoader(classLoader), this.cache.proxiedInterfaces, this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public Class<?> getProxyClass(@Nullable ClassLoader classLoader) {
		return Proxy.getProxyClass(determineClassLoader(classLoader), this.cache.proxiedInterfaces);
	}

	/**
	 * 确定是否已建议使用JDK引导类加载器或平台类加载器 -> 使用能够看到Spring基础设施类的更高级别类加载器。
	 */
	private ClassLoader determineClassLoader(@Nullable ClassLoader classLoader) {
		if (classLoader == null) {
			// JDK bootstrap loader -> use spring-aop ClassLoader instead.
			return getClass().getClassLoader();
		}
		if (classLoader.getParent() == null) {
			// Potentially the JDK platform loader on JDK 9+
			ClassLoader aopClassLoader = getClass().getClassLoader();
			ClassLoader aopParent = aopClassLoader.getParent();
			while (aopParent != null) {
				if (classLoader == aopParent) {
					// Suggested ClassLoader is ancestor of spring-aop ClassLoader
					// -> use spring-aop ClassLoader itself instead.
					return aopClassLoader;
				}
				aopParent = aopParent.getParent();
			}
		}
		// Regular case: use suggested ClassLoader as-is.
		return classLoader;
	}


	/**
	 * jdk代理的这部分代码和cglib的大部分逻辑一致，
	 * spring对此的解释为：抽象出来更优雅，但是会代理10%的性能损耗，所以spring提供了两份一致的代码
	 *
	 * proxy：代理实例
	 * method：代理实例上调用的接口方法对应的method实例
	 * args：方法调用时传入的参数
	 *
	 * 此处重点分析，后续Cglib则可参考此方法的分析
	 */
	@Override
	public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object oldProxy = null;
		boolean setProxyContext = false;

		TargetSource targetSource = this.advised.targetSource;
		Object target = null;

		try {
			// 通常情况jdk不会对equals和hashCode方法做代理，所以特殊处理
			// 如果equalsDefined为false（没有接口定义过equals方法），那就交给此类JdkDynamicoAopProxy的equals方法处理
			// hashCode也同理
			if (!this.cache.equalsDefined && AopUtils.isEqualsMethod(method)) {
				// The target does not implement the equals(Object) method itself.
				return equals(args[0]);
			}
			else if (!this.cache.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
				// The target does not implement the hashCode() method itself.
				return hashCode();
			}
			// 下面两段代码的处理：DecoratingProxy接口和Advised接口的方法最终都交给config advised（AdvisedSupport类）去处理
			else if (method.getDeclaringClass() == DecoratingProxy.class) {
				// There is only getDecoratedClass() declared -> dispatch to proxy config.
				return AopProxyUtils.ultimateTargetClass(this.advised);
			}
			else if (!this.advised.isOpaque() && method.getDeclaringClass().isInterface() &&
					method.getDeclaringClass().isAssignableFrom(Advised.class)) {
				// Service invocations on ProxyConfig with the proxy config...
				return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
			}
			// 最终方法返回值
			Object retVal;
			// 是否暴露代理对象，默认为false，可以设置为true，如果为true则意味着允许线程内共享代理对象
			// 线程内共享：是指在代理方法执行期间，同一个线程可以通过AopContext获取该代理对象
			if (this.advised.isExposeProxy()) {
				// Make invocation available if necessary.
				// 此处replace，后续reset
				oldProxy = AopContext.setCurrentProxy(proxy);
				setProxyContext = true;
			}

			// Get as late as possible to minimize the time we "own" the target,
			// in case it comes from a pool.
			// 尽可能晚的获取目标对象，减少持有时间，防止对象来自资源池
			target = targetSource.getTarget();
			Class<?> targetClass = (target != null ? target.getClass() : null);

			// Get the interception chain for this method.
			// 获取作用于这个方法的所有拦截器链。参见DefaultAdvisorChainFactory#getInterceptorsAndDynamicInterceptionAdvice方法
			// 会根据Pointcut表达式匹配方法，因此每个方法都会进入到这里，只是有很多方法的chain为空
			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

			// Check whether we have any advice. If we don't, we can fall back on direct
			// reflective invocation of the target, and avoid creating a MethodInvocation.
			// 检查是否存在advice，如果没有则回退到直接调用目标方法，避免创建MethodInvocation对象
			if (chain.isEmpty()) {
				// We can skip creating a MethodInvocation: just invoke the target directly
				// 可以跳过创建MethodInvocation：直接调用target对象即可
				// 对参数进行适配，主要处理一些数组类型的参数，表示多个参数还是一个（如：可变参数）
				// Note that the final invoker must be an InvokerInterceptor so we know it does
				// nothing but a reflective operation on the target, and no hot swapping or fancy proxying.
				@Nullable Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
				// 直接调用目标方法
				retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
			}
			else {
				// 创建一个MethodInvocation，此处为ReflectiveMethodInvocation，最终通过它取执行advice逻辑
				MethodInvocation invocation =
						new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
				// Proceed to the joinpoint through the interceptor chain.
				// 此处会执行所有的interceptor chain 拦截器链，交给AOP联盟的MethodInvocation取处理，实现为Spring的ReflectiveMethodInvocation
				retVal = invocation.proceed();
			}

			// 获取返回值类型
			Class<?> returnType = method.getReturnType();
			if (retVal != null && retVal == target &&
					returnType != Object.class && returnType.isInstance(proxy) &&
					!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
				//一些条件判断，如果返回值不为空，且为表表对象的话，使用proxy作为返回
				retVal = proxy;
			}
			// 返回值为null，但是定义为基本类型
			else if (retVal == null && returnType != void.class && returnType.isPrimitive()) {
				throw new AopInvocationException(
						"Null return value from advice does not match primitive return type for: " + method);
			}
			if (COROUTINES_REACTOR_PRESENT && KotlinDetector.isSuspendingFunction(method)) {
				return COROUTINES_FLOW_CLASS_NAME.equals(new MethodParameter(method, -1).getParameterType().getName()) ?
						CoroutinesUtils.asFlow(retVal) : CoroutinesUtils.awaitSingleOrNull(retVal, args[args.length - 1]);
			}
			// 返回结果
			return retVal;
		}
		finally {
			// 释放target
			if (target != null && !targetSource.isStatic()) {
				// Must have come from TargetSource.
				targetSource.releaseTarget(target);
			}
			// 重置AopContext
			if (setProxyContext) {
				// Restore old proxy.
				AopContext.setCurrentProxy(oldProxy);
			}
		}
	}


	/**
	 * AOP实现的equals
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		JdkDynamicAopProxy otherProxy;
		if (other instanceof JdkDynamicAopProxy jdkDynamicAopProxy) {
			otherProxy = jdkDynamicAopProxy;
		}
		else if (Proxy.isProxyClass(other.getClass())) {
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			if (!(ih instanceof JdkDynamicAopProxy jdkDynamicAopProxy)) {
				return false;
			}
			otherProxy = jdkDynamicAopProxy;
		}
		else {
			// Not a valid comparison...
			return false;
		}

		// If we get here, otherProxy is the other AopProxy.
		return AopProxyUtils.equalsInProxy(this.advised, otherProxy.advised);
	}

	/**
	 * AOP实现的hashCode方法
	 */
	@Override
	public int hashCode() {
		return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
	}


	//---------------------------------------------------------------------
	// Serialization support 序列化
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();

		// Initialize transient fields.
		this.cache = new ProxiedInterfacesCache(this.advised);
	}


	/**
	 * 用于保存所有被代理接口及其派生元数据的持有者，这些数据将被缓存在Advised Support#proxyMetadataCache中
	 */
	private static final class ProxiedInterfacesCache {
		// 代理的接口集合
		final Class<?>[] proxiedInterfaces;
		// 标记equals和hashCode方法是否在接口上被定义
		final boolean equalsDefined;

		final boolean hashCodeDefined;

		ProxiedInterfacesCache(AdvisedSupport config) {
			// 获取config的代理接口 proxied interface，
			// 会根据条件额外添加三个接口：SpringProxy、Advised、DecoratingProxy接口
			this.proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(config, true);

			// Find any {@link #equals} or {@link #hashCode} method that may be defined
			// on the supplied set of interfaces.
			// 确认接口上是否定义了equals 和 hashCode方法
			boolean equalsDefined = false;
			boolean hashCodeDefined = false;
			for (Class<?> proxiedInterface : this.proxiedInterfaces) {
				Method[] methods = proxiedInterface.getDeclaredMethods();
				for (Method method : methods) {
					if (AopUtils.isEqualsMethod(method)) {
						equalsDefined = true;
						if (hashCodeDefined) {
							break;
						}
					}
					if (AopUtils.isHashCodeMethod(method)) {
						hashCodeDefined = true;
						if (equalsDefined) {
							break;
						}
					}
				}
				if (equalsDefined && hashCodeDefined) {
					break;
				}
			}
			this.equalsDefined = equalsDefined;
			this.hashCodeDefined = hashCodeDefined;
		}
	}

}
