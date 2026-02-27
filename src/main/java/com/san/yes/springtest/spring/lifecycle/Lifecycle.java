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
 * 一个定义了start/stop方法的生命周期控制通用接口，通常用于控制异步处理
 * 注意：此接口未隐含特定的自动启动语义，可以考虑SmartLifecycle实现此目的
 *
 * 可以同时由组件（通常为context中的bean）和容器（通常为ApplicationContext）实现
 * 容器会将start/stop信号传播给容器内所有适用组件.
 *
 * 该接口可直接调用，也可以用于JMX管理操作
 */
public interface Lifecycle {

	/**
	 * 启动此组件
	 * 1. 如果组件已经运行中，则不应抛出异常
	 * 2. 如果是容器，则应该向所有适用组件（即使非自动启动组件）传播强制启动信号
	 */
	void start();

	/**
	 * 停止该组件，通常为同步方式，确保方法返回时确保组件已经完全停止，
	 * 当需要异步停止时，应考虑实现SamrtLifecycle#stop(runnable)方法
	 * 注意：停止通知不保证在销毁之前到达。
	 * 正常关闭过程中：Lifecycle bean先收到stop通知，然后才会被传播销毁回调。
	 * 但是：在热刷新或者刷新被终止时，bean的销毁方法可能会被回调，但是没有stop信号
	 */
	void stop();

	/**
	 * 检查当前组件是否在运行中
	 * 如果是容器：只有当所有适用组件都在运行中时才返回true
	 */
	boolean isRunning();

}
