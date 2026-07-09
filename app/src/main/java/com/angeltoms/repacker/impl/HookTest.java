/*
 * Copyright AngelToms
 * SPDX-License-Identifier: Apache-2.0
 */

package com.angeltoms.repacker.impl;

import android.os.Build;
import android.util.Log;

import com.angeltoms.repacker.hook.MethodHook;

import java.lang.reflect.Method;

/**
 * @Atuthor: AngelToms
 */

public class HookTest {

    private final static String TAG = "HookTest";

    public void test1(String s1, int s2) {
        String s = s1 + " world " + s2;
        Log.i(TAG , "Call method test1 : " + s);
    }

    public void test2(String s1, int s2) {
        String s = s1 + " " + s2;
        Log.i(TAG , "Call method test2 : " + s);
    }

    public String test3(String s1) {
        Log.i(TAG , "Call method test3 : " + s1);
        return s1;
    }

    public String test4(String str) {
        Log.i(TAG , "Call method test4 : " + str);
        return str + "wwwwwwhhhhhhhhhhhh";
    }

    public void testHook() throws  Exception{

        Log.i(TAG, "===========================================================");
        test1("fuck", 10);
        Log.i(TAG, "===========================================================");
        MethodHook methodHook1 = new MethodHook();
        methodHook1.setApiLevel(Build.VERSION.SDK_INT);

        Method test1m = this.getClass().getDeclaredMethod("test1", String.class, int.class);
        Method test2m = this.getClass().getDeclaredMethod("test2", String.class, int.class);

        methodHook1.hookAndBack(test1m, test2m);

        test1("hwol", 18);
        Log.i(TAG, "===========================================================");
        methodHook1.callOrigin(this, "hello", 3);
        Log.i(TAG, "===========================================================");
        test1("xxxxx", 20);
        Log.i(TAG, "===========================================================");
        methodHook1.restory(test1m);
        test1("dashag", 99);
        Log.i(TAG, "===========================================================");
        MethodHook methodHook2 = new MethodHook();
        methodHook2.setApiLevel(Build.VERSION.SDK_INT);

        Method test3m = this.getClass().getDeclaredMethod("test3", String.class);
        Method test4m = this.getClass().getDeclaredMethod("test4", String.class);

        String rs = test3("ttttttttttttttt");
        Log.i(TAG, rs);
        Log.i(TAG, "===========================================================");

        methodHook2.hookAndBack(test3m, test4m);

        rs = test3("kkkkkkkkkkkkkkkkkkkk");
        Log.i(TAG, rs);
        Log.i(TAG, "===========================================================");
        rs = methodHook2.callOrigin(this, "gggggggggggggggggggggggggg");
        Log.i(TAG, rs);
        Log.i(TAG, "===========================================================");
        rs = test3("ttttttttttttttt");
        Log.i(TAG, rs);

        methodHook1.restory(test3m);
        Log.i(TAG, "===========================================================");
        rs = test3("ttttttttttttttt");
        Log.i(TAG, rs);
        Log.i(TAG, "===========================================================");
    }
}
