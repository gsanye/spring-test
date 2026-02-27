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

package org.springframework.beans;

import java.beans.PropertyDescriptor;

/**
 * Spring底层JavaBean基础摄氏核心接口
 * 通常不会直接使用，而是通过BeanFactory或DataBinder隐式使用
 * 1. 提供了操作和分析标准JavaBean的操作：能够获取和设置property value属性值、获取property descriptors、以及查询properties的读写属性
 * 2. 该接口支持嵌套属性，允许设置子属性的值，且没有深度限制
 * 3. BeanWrapper的extractOldValueForEditor默认设置为false，避免getter方法产生副作用。true则将当前属性值暴露给自定义编辑器。
 */
public interface BeanWrapper extends ConfigurablePropertyAccessor {

	/**
	 * 为数组和集合指定自动增长限制，普通BeanWrapper默认没有限制
	 */
	void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

	/**
	 * 返回数组和集合的自动扩展限制
	 */
	int getAutoGrowCollectionLimit();

	/**
	 * 返回此对象包装的实例
	 */
	Object getWrappedInstance();

	/**
	 * 返回被包装bean实力的类型
	 */
	Class<?> getWrappedClass();

	/**
	 * 获取被包装对象的PropertyDescriptor(通过标准的JavaBean自省机制确定)
	 */
	PropertyDescriptor[] getPropertyDescriptors();

	/**
	 * 获取被包装对象指定property的property-descriptor属性描述符
	 */
	PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException;

}
