/*
 * Copyright AngelToms
 * SPDX-License-Identifier: Apache-2.0
 */
package com.angeltoms.repacker.hook;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import android.util.Log;
 
/**
 * hook功能类，你应该调用这里的方法，和初始化这个类，而不是ArtMethod
 * hook功能的实现，提供功能：
 * 直接hook，原函数功能丢失，无法在调用原函数
 * 常规hook，可以实现调用原函数，同时新调用将执行hook的逻辑
 * 去除hook， 恢复原函数，恢复至hook前状态
 * @Author: AngelToms
 * @version: 0.2
 * 
 */
public class MethodHook/*<T>*/ {

	static {
		System.loadLibrary("repacker_hook");
	}
	
	private final static String TAG = "MethodHook";

	private  ArtMethod artMethod = new ArtMethod();
	private final Object hookLock = new Object();
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Warning! 实现方式有很多，这只是其中一种实现，我认为实现起来比较简单，仅供参考
	// ## 圈起来的变量，你最好别动，除非你真的知道它是做什么用的
	// native层备份原方法的指针. 实现原函数调用有两种方式，一种是把backMethodPtr传给native直接执行;
	// 另一种是把backMethodPtr转成Method在java层执行，我觉得在java层执行更好实现代码, 还能利用虚拟机的机制
	private long backMethodPtr; // native层实际可以通过反射拿到它，但是还是传给native层更方便，少写一些代码
	private Method backMethod; // 对应backMethodPtr转换，纯hook前的原函数拷贝
	private Method originMethod; // 用于保存原方法，但hook后它会发生变化，之所以保留是因为native会用到它的属性
	// private T receiver;
	// public void setReceiver(T receiver) {
	// 	this.receiver = receiver;
	// }
	// 实际可以实现对一个类的多个函数hook
	// 方法是把上面三个变量封装成一个类,假设叫BackMethod，然后用Map<long, BackMethod> 就可以实现一个类的多个函数hook
	// 我嫌麻烦，这里就不实现了， 想实现的同学自己取改代码吧
	////////////////////////////////////////////////////////////////////////////////////////////////////////

	public MethodHook() {
	}

	/**
	 * 不带备份源函数的hook
	 * 原函数会被替换，无法在调用回原函数
	 * @param src，源函数
	 * @param dst，目标函数
	 * @return，是否成功
	 */
	public boolean hook(Method src, Method dst) {
		synchronized (hookLock) {
			return artMethod.cloneMethod(src, dst);
		}
	}
	
	/**
	 * TODO; 这个设计在非系统的应用中能跑通（hook + 调用原函数），但是针对系统接口，hook能成功，但不能调用原函数
	 * 毕竟我们偷工减料了太多， 对于虚拟机的一些检查或者gc我们没有考虑，或许
	 * 	因此暂时先不用此接口，用其他方法弥补
	 * 带拷贝源函数的hook
	 * 支持hook后调回原函数
	 * @param src，源函数
	 * @param dst，目标函数
	 * @return，是否成功
	 */
	public boolean hookAndBack(Method src, Method dst) {
		synchronized (hookLock) {
			if (!backedMethod(src)) {
				Log.e(TAG, "clone origin method failed");
				return false;
			}
			return artMethod.cloneMethod(src, dst);
		}
	}
	
	/**
	 * 备份原函数；注意只能hook一次， 如果想重新hook请先调用restory函数，否则会产生很多内存垃圾
	 * 执行后原函数被备份到内存
	 * @param src 要被备份的方法
	 * @return 成功返回true
	 */
	private boolean backedMethod(Method src) {
		originMethod = src;
		backMethodPtr = artMethod.cloneMethod1(src);
		return backMethodPtr == 0 ? false : true;
	}

	/**
	 * TODO; 后续待实现
	 * @param src
	 * @param dst
	 * @return
	 */
	private boolean backedMethod(Method src, Method dst) {
		return false;
	}

	/**
	 * 调用hook前的原函数
	 */
	public <T> T callOrigin(Object receiver, Object... args) {
		if(backMethod == null) {
			if(originMethod == null) {
				Log.e(TAG, "You must call hookAndBack first, If you call hook first, this will be fail!");
				return null;
			}
			Log.d(TAG, originMethod.toGenericString());
			backMethod = artMethod.getBackMethod(backMethodPtr,
													originMethod.getDeclaringClass(),
													Modifier.isStatic(originMethod.getModifiers()));
		}
		if (backMethod == null) {
			Log.e(TAG, "Call origin " + originMethod.toGenericString() + " fail");
			return null;
		}

		// 仅用于打印信息
		Log.d(TAG, backMethod.getDeclaringClass().getCanonicalName());
		Log.d(TAG, backMethod.getName());
		Log.d(TAG, Arrays.toString(
				backMethod.getParameterTypes()));

		try { // 这是核心的调用
			return (T) backMethod.invoke(receiver, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 还原hook函数
	 * @param src
	 * @return
	 */
	public boolean restory(Method src) {
		if (src == null) {
			src = originMethod;
		}
		synchronized (hookLock) {
			if (artMethod.restoryMethod(src, backMethodPtr, false)) {
				backMethod = null;
				originMethod = null;
				return true;
			}
			return false;
		}
	}

	/**
	 * 短暂还原函数
	 * @return
	 */
	public boolean restoryShort(Method src) {
		if (src == null) {
			src = originMethod;
		}
		synchronized (hookLock) {
			if (artMethod.restoryMethod(src, backMethodPtr, true)) {
				backMethod = null;
				originMethod = null;
				return true;
			}
			return false;
		}
	}
	
	/**
	 * 设置api level，用于判断底层art method类型
	 * @param l 对于api level
	 */
	public void setApiLevel(int l) { // int apilevel = Build.VERSION.SDK_INT;
		artMethod.setApiLevel(l);
	}
}
