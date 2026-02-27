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
 * context用于处理Lifecycle bean的策略接口
 */
public interface LifecycleProcessor extends Lifecycle {

	/**
	 * 通知自动启动组件context刷新,即start
	 */
	default void onRefresh() {
		start();
	}

	/**
	 * 通知自动启动组件context重启，即先stop随后start
	 */
	default void onRestart() {
		stop();
		start();
	}

	/**
	 * 通知自动停止组件context暂停，即stop
	 * Notification of context pause for auto-stopping components.
	 * @since 7.0
	 * @see ConfigurableApplicationContext#pause()
	 */
	default void onPause() {
		stop();
	}

	/**
	 * 通知自动停止组件context关闭，即stop
	 */
	default void onClose() {
		stop();
	}

}
