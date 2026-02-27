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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用对处理使用 AspectJ 的 @Aspect 注解标记的组件的支持，
 * 这类似于 Spring 中 <aop:aspectj-autoproxy> XML 元素所提供的功能。该注解应使用于 @Configuration 类
 *
 * 请注意，@Aspect 标注的 bean 可以像其他组件一样被组件扫描发现。只需同时使用 @Aspect 和 @Component 注解即可：
 *
 * 注意：@EnableAspectJAutoProxy 仅适用于其本地应用上下文，从而允许在不同层次选择性地代理 bean。
 * 如果你需要在多个层次应用其行为（例如，公共的根 Web 应用上下文和任何独立的 DispatcherServlet 应用上下文），
 * 则需要在每个上下文中重新声明 @EnableAspectJAutoProxy。
 *
 * 此功能要求 classpath 上存在 aspectjweaver。虽然 aspectjweaver 作为 spring-aop 的依赖项在一般情况下是可选的，
 * 但它是 @EnableAspectJAutoProxy 及其底层功能所必需的。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies. The default is {@code false}.
	 * 指示是否创建基于子类（CGLIB）的代理，而不是创建基于标准Java接口的代理。默认值为 false。
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate that the proxy should be exposed by the AOP framework as a {@code ThreadLocal}
	 * for retrieval via the {@link org.springframework.aop.framework.AopContext} class.
	 * Off by default, i.e. no guarantees that {@code AopContext} access will work.
	 * @since 4.3.1
	 * 指示AOP框架是否应将代理暴露为ThreadLocal，以便通过org.springframework.aop.framework.AopContext类进行检索。
	 * 默认情况下是关闭的，即不保证AopContext的访问能够正常工作。
	 */
	boolean exposeProxy() default false;

}
