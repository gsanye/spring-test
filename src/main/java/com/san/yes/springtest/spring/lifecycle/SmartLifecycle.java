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

package org.springframework.context;

/**
 * SmartLifecycle是Lifecycle的扩展，用于那些要求在context容器 refresh刷新 或 shutdown关闭 时按照特性顺序启动的对象
 *
 * 1. isAutoStartup()：指示此对象是否应该在容器刷新时启动
 * 2. stop(runnable)：用于异步关闭的对象，任何实现必须在关闭完成时回调run()方法以避免整体context关闭过程中不必要的延迟
 *
 * 顺序：
 * 此接口扩展Phased，getPhased方法返回只是此Lifecycle组件应该在那个阶段启动和停止
 * 启动：从小到大
 * 停止：从大到小
 * 相同的phased则内部没有顺序
 * 如：B依赖A，则phased A应该小于B，即 A先于 B创建 ，A晚于B 销毁
 *
 * 显示depends-on：显示的depends-on顺序优先于phased顺序，依赖bean总时晚于被依赖bean启动，且早于被依赖bean停止
 *
 * Lifecycle组件默认phased为0，SmartLifecycle的phased如果为负数则早于它们启动，如果为正值则晚于他们启动
 *
 * 由于对自动启动的支持，SamrtLifecycle bean实例通常在context启动时初始化，因此lazy-init懒加载对它影响非常有限
 */
public interface SmartLifecycle extends Lifecycle, Phased {

	/**
	 * SmartLifecycle 的默认阶段：Integer.MAX_VALUE。
	 * 这与常规 Lifecycle 实现所关联的通常阶段 0 不同，
	 * 从而将通常自动启动的 SmartLifecycle Bean 放入一个较晚的启动阶段和较早的关闭阶段。
	 * 请注意，某些 SmartLifecycle 组件具有不同的默认阶段：例如，执行器（executors）/调度器（schedulers）的默认阶段为 Integer.MAX_VALUE / 2。
	 */
	int DEFAULT_PHASE = Integer.MAX_VALUE;


	/**
	 * 如果此生命周期组件应在包含的 ApplicationContext 被刷新或重启时由容器自动启动，则返回 true。
	 * 值为 false 表示该组件应通过显式的 start() 调用（而不是自动启动）来启动，这与普通的 Lifecycle 实现类似。
	 * 默认实现返回 true。
	 */
	default boolean isAutoStartup() {
		return true;
	}

	/**
	 * 如果此生命周期组件能够参与重启序列（即在重启过程中接收相应的 stop() 和 start() 调用，中间可能会有一个暂停），则返回 true。
	 *
	 * 值为 false 表示该组件在暂停场景中更倾向于被跳过，既不会接收 stop() 调用，也不会接收后续的 start() 调用，
	 * 这与普通的 Lifecycle 实现类似。该组件仅在关闭或显式执行上下文范围内的停止操作时接收 stop() 调用，而不会在暂停时接收该调用。
	 *
	 */
	default boolean isPauseable() {
		return true;
	}

	/**
	 * 指示一个生命周期组件如果当前正在运行，则必须停止。
	 * 提供的回调由 `LifecycleProcessor` 用于支持所有具有相同关闭顺序值的组件的有序（并且可能是并发的）关闭。
	 * 该回调必须在 `SmartLifecycle` 组件确实停止之后执行。
	 *
	 * `LifecycleProcessor` 只会调用 `stop` 方法的此变体；也就是说，除非在此方法的实现中明确委托，否则不会为 `SmartLifecycle` 实现调用 `Lifecycle.stop()`。
	 *
	 * 默认实现会委托调用 `stop()` 方法，并在调用线程中立即触发给定的回调。
	 * 请注意，这两个操作之间没有同步机制，因此自定义实现至少可能希望将相同的步骤放在它们的公共生命周期监视器（如果有的话）中。
	 */
	default void stop(Runnable callback) {
		stop();
		callback.run();
	}

	/**
	 * 返回此生命周期对象应运行的阶段。
	 * 默认实现返回 DEFAULT_PHASE，以便让 stop() 回调在常规的 Lifecycle 实现之前执行。
	 */
	@Override
	default int getPhase() {
		return DEFAULT_PHASE;
	}

}
