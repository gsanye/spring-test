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

package org.springframework.aop.target;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;

/**
 * 避免在Spring容器启动时初始化target对象，将bean标记为lazy-init，
 * 可以在ApplicationContext或BeanFactory启动时不被实例化
 */
@SuppressWarnings("serial")
public class LazyInitTargetSource extends AbstractBeanFactoryBasedTargetSource {
	// target对象实例
	private @Nullable Object target;

	// synchronized同步锁
	@Override
	public synchronized Object getTarget() throws BeansException {
		if (this.target == null) {
			// 从BeanFactory中获取bean（创建Bean）
			// 主意：此处的BeanFactory为内部BeanFactory，原始BeanFactory为parent
			// 因为原始BeanFactory中已经存在此target的aop-proxy对象
			this.target = getBeanFactory().getBean(getTargetBeanName());
			postProcessTargetObject(this.target);
		}
		return this.target;
	}

	/**
	 * 子类可以重写此方法，以便在目标对象首次加载时对其执行额外的处理。
	 */
	protected void postProcessTargetObject(Object targetObject) {
	}

}
