
package com.san.yes.springtest.spring.postprocessor;

import org.springframework.beans.BeansException;

/**
 * 添加 before-destruction 回调方法的子接口
 * 通常：
 * 用于调用bean的自定义销毁回调，匹配对应的初始化回调
 */
public interface DestructionAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * 应用于bean销毁之前，调用自定义销毁回调方法，
	 * 适用于完全由容器管理生命周期lifecycle的bean，通常是单例singleton和scoped bean
	 */
	void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException;

	/**
	 * 确定给定bean是否需要该处理器post-processor进行销毁
	 * 返回false：不需要
	 */
	default boolean requiresDestruction(Object bean) {
		return true;
	}
}
