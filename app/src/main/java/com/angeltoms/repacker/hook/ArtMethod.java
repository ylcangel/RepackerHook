/*
 * Copyright AngelToms
 * SPDX-License-Identifier: Apache-2.0
 */

package com.angeltoms.repacker.hook;

import java.lang.reflect.Method;

/**
 * MethodHook辅助类， native 对应art_method.h 和 art_method.cpp
 * 你最好不要直接调用这个类，hook功能都是通过MethodHook提供的
 * 没啥变化对比第一版，部分功能后期在填充和修复
 * 此代码功能只是实验性的
 * @Author: AngelToms
 * @version: 0.2
 */
public class ArtMethod {

	public ArtMethod() {}
	
	/**
	 * 克隆方法用的,同时也是hook的主方法，此方法完成native层原函数method结构替换（执行新代码逻辑）
	 * 此方法完成src 到 dst方法的替换，用于hook原函数
	 * @param src 即将被hook的方法
	 * @param dst 替换原方法的方法
	 * @return 是否成功
	 */
	public native boolean cloneMethod(Method src, Method dst);

	/**
	 * 克隆方法用的,同时也是hook的主方法，此方法完成native层原函数method结构保留
	 * 用于保留原函数
	 * @param src 原方法
	 * @return 是否成功
	 */
	public native long cloneMethod1(Method src);

	/**
	 * 获取备份原函数的Method结构
	 * @param methodPtr
	 * @param clazz 原函数对应类
	 * @param isStatic 原函数是否是static函数
	 * @return
	 */
	public native Method getBackMethod(long methodPtr, Class clazz, boolean isStatic);

	/**
	 * 用于去除函数hook，恢复原函数逻辑
	 * @param src 原函数
	 * @return
	 */
	public native boolean restoryMethod(Method src, long methodPtr, boolean isShort);
	
	/**
	 * 设置api level，用于判断底层art method类型
	 * @param l 对于api level
	 */
	public native void setApiLevel(int l);
	
}