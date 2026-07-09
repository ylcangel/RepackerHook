/*
 * Copyright AngelToms
 * SPDX-License-Identifier: Apache-2.0
 */

package com.angeltoms.repacker.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Log;

import com.angeltoms.repacker.hook.MethodHook;

/**
 * 替换系统签名
 * 提供了两种方式，依赖context版和不依赖context版，效果是一样的
 * @Author: AngelToms
 * @Version: 0.2
 *
 */

public class HookSignature {
    private static final String TAG = "HookSignature";
    private static volatile boolean sHooked = false;
    // sSignature是测试时用的签名
    public static String sSignature = "3082025d308201c6a00302010202044c032728300d06092a864886f70d01010505003072310b300906035504061302636e3110300e060355040813076265696a696e673110300e060355040713076265696a696e673111300f060355040a13086175746f6e617669311a3018060355040b13116d6f62696c65206465706172746d656e743110300e060355040313076d696e696d61703020170d3130303533313033303430385a180f32303635303330333033303430385a3072310b300906035504061302636e3110300e060355040813076265696a696e673110300e060355040713076265696a696e673111300f060355040a13086175746f6e617669311a3018060355040b13116d6f62696c65206465706172746d656e743110300e060355040313076d696e696d617030819f300d06092a864886f70d010101050003818d0030818902818100ac245c9af4bad32ed007e7ba5ea35c03e26e9e313e40a76f3e579fa8e612c28a63e90786b2de9cc372ce87c55e50e2f5e6ec463366e088773ca48df7d569f0255c844976d3700aadbf072306423bf32367f07eb5250a90ba0bd56d7aea185036b51809068ad733fb2c1c2cef0f979009d68de73ea100e6cd8002feaa0267b4970203010001300d06092a864886f70d010105050003818100a52066c0edfbc9ee5a5ff4caff399db11a741b31542d6e69cbcfef03597678a254b4102a3c42fbe2903af432f6f8fddcad8704fe2e952c39b5bbfac892019f32c67bdbd081f40c5d634437f1e6ebe27d767c621b95524c807b0d1318518f721c46103b831c8bbcef1b7192c6d6cebe1c83c6f5aa046ad28dbc6f01385a90601c";
    public static String sMockSignature = "Angeltoms tell you, this will be true!"; // 替换为你想要的伪造签名 Hex 字符串toCharString

    public static PackageManager sPmg = null;

    private Context context;

    // 需要被替换
    private static String sOldSourceDir = null;
    // 一般很少通过获取签名方式获取ApplicationInfo，这里预留，但是重打包程序暂时忽略这里的替换
    private static String sNewSourceDir = null;
    private static PackageInfo sOldPkgInfo = null;
    private static PackageInfo sNewPkgInfo = null;

    private static String sCurrentPackageName = null;

    // 持有我们的 MethodHook 实例，以便在静态拦截函数中能够原路调用原函数
    private static MethodHook sPmGetPkgInfoHook = new MethodHook();

    public HookSignature() {
    }
    // 依赖context版本
    public HookSignature(Context context) {
        this.context = context;
        if (!init()) {
            throw new RuntimeException("HookSignature Init failed");
        }
    }

    public boolean init() { // 利用此方法提前备份调用原函数返回的参数，以弥补回调原函数
        try {
            PackageManager pm = context.getPackageManager();
            sPmg = pm;
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            sOldPkgInfo = packageInfo;
            sNewPkgInfo = new PackageInfo();
            clonePackageInfoLite1(sOldPkgInfo, sNewPkgInfo);
            sOldSourceDir = packageInfo.applicationInfo.sourceDir;
            sCurrentPackageName = getCurrentPackageName();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean initWithoutContext() {
        try {
            Class<?> pmClazz = Class.forName("android.app.ApplicationPackageManager");
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = atClass.getDeclaredMethod("currentActivityThread");
            Object activityThread = currentActivityThread.invoke(null);

            // 这里虽然我们获取的是SystemContext，不是当前应用Application依赖的Context
            // 但不影响最终逻辑，因为pm是共用的（后面都对应着PMS）
            Method getSystemContext = atClass.getDeclaredMethod("getSystemContext");
            Context context = (Context)getSystemContext.invoke(activityThread);

            PackageManager pm = context.getPackageManager();
            sPmg = pm;
            Method method = null;
            try {
                method = pmClazz.getDeclaredMethod("getPackageInfo", String.class, int.class);
            } catch (NoSuchMethodException e) {
                // 如果某些极个别 Android 5.0 ROM 在子类没写这个方法（抛了异常）
                // 说明它完全复用了父类，此时我们再退回 getMethod
                method = pmClazz.getMethod("getPackageInfo", String.class, int.class);
            }

            if (method == null) // 应该不会在获取失败了，如果还是失败，那就看看rom代码吧
                return false;

            PackageInfo pi = (PackageInfo) method.invoke(pm, getCurrentPackageName(), PackageManager.GET_SIGNATURES);
            if (pi == null)
                return false;

            sOldPkgInfo = pi;
            sNewPkgInfo = new PackageInfo();
            clonePackageInfoLite(sOldPkgInfo, sNewPkgInfo);
            sOldSourceDir = pi.applicationInfo.sourceDir;
            sCurrentPackageName = getCurrentPackageName();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 无 Context 懒加载包名获取函数
     */
    private static String getCurrentPackageName() {
        if (sCurrentPackageName != null) {
            return sCurrentPackageName;
        }

        try {
            Class<?> atClazz = Class.forName("android.app.ActivityThread");
            Method m = atClazz.getDeclaredMethod("currentPackageName");
            m.setAccessible(true);
            String pkg = (String) m.invoke(null);

            if (pkg != null) {
                sCurrentPackageName = pkg;
                Log.d(TAG, "Current package name: " + sCurrentPackageName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Get current package name fail");
            e.printStackTrace();
        }
        return sCurrentPackageName;
    }

    /**
     * hook pm.getPackageInfo(String packageName, int flags)
     * 程序代码如下：
     * PackageManager pm = getPackageManager();
     * PackageInfo packageInfo = pm.getPackageInfo(String packageName, int flags)
     * 这里存在一个问题，不能callback导致flags传入任何都被当作GET_SIGNATURES，上面初始化时
     * 按照GET_SIGNATURES初始化的
     * //## 仅用于绕过签名校验，其他flags需要等待修复
     */
    public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {
        Log.i(TAG, "Call getPackageInfo - package name: " + packageName + ", flags: " + flags);
        // 会失败; TODO; 等10年后Android没了在修复，要不你就用别人成熟的框架吧
        // PackageInfo pi = sPmGetPkgInfoHook.callOrigin(sPmg, packageName, flags);
        // Log.i(TAG, pi.applicationInfo.dataDir);

        if (sCurrentPackageName != null && sCurrentPackageName.equals(packageName)) {
            // 仅flags == PackageManager.GET_SIGNATURES时
            if ((flags & PackageManager.GET_SIGNATURES) != 0) {
                if (sNewPkgInfo.signatures == null) {
                    Signature mockSignature = new Signature(sMockSignature);
                    sNewPkgInfo.signatures = new Signature[]{mockSignature};
                    Log.d(TAG, "Pm getPackageInfo replace Signatrue end!");
                }
            }

            // 替换applicationInfo.sourceDir和applicationInfo.publicSourceDir
            // 通过ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            // applicationInfo.sourceDir或者applicationInfo.publicSourceDir获取 APK路径
            // 去主动解析签名
            // 当然这里的hook仅用于通过getPackageInfo获取 PackageInfo，在获取ApplicationInfo的方式的场景
            if (sNewPkgInfo.applicationInfo != null && sNewSourceDir != null) {
                sNewPkgInfo.applicationInfo = null;
            }
            // 如果只想替换签名，无需给sNewResDir赋值即可，这样下面的逻辑就不会执行
            // 下面逻辑是针对构造fake包场景
            if (sNewSourceDir != null) {
                if (sNewPkgInfo.applicationInfo == null) {
                    ApplicationInfo forgedAppInfo = new ApplicationInfo(sOldPkgInfo.applicationInfo);
                    forgedAppInfo.sourceDir = sNewSourceDir;
                    forgedAppInfo.publicSourceDir = sNewSourceDir;
                    sNewPkgInfo.applicationInfo = forgedAppInfo;

                    Log.d(TAG, "GetPackageInfo replace ApplicationInfo end!");
                }
            }

            Log.d(TAG, "Pm getPackageInfo end!");
            return sNewPkgInfo;
        }
        return sOldPkgInfo;
    }

    /**
     * 辅助函数：轻量级拷贝 PackageInfo
     */
    private static void clonePackageInfoLite(PackageInfo src, PackageInfo dst) {
        dst.packageName = src.packageName;
        dst.versionCode = src.versionCode;
        dst.versionName = src.versionName;
        dst.sharedUserId = src.sharedUserId;
        dst.sharedUserLabel = src.sharedUserLabel;
        dst.applicationInfo = src.applicationInfo;
        dst.firstInstallTime = src.firstInstallTime;
        dst.lastUpdateTime = src.lastUpdateTime;
        dst.activities = src.activities;
        dst.receivers = src.receivers;
        dst.services = src.services;
        dst.providers = src.providers;
        dst.instrumentation = src.instrumentation;
        dst.permissions = src.permissions;
        dst.requestedPermissions = src.requestedPermissions;
        dst.requestedPermissionsFlags = src.requestedPermissionsFlags;
        dst.signatures = null; // 等待替换
    }

    /**
     * 兼容性更好，复制更全
     * 另一种实现
     */
    private static void clonePackageInfoLite1(PackageInfo src, PackageInfo dst) {
        // 获取 PackageInfo 所有的公开成员变量（字段）
        Field[] fields = PackageInfo.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                int mod = field.getModifiers();

                // 跳过 static 字段
                if (Modifier.isStatic(mod)) {
                    continue;
                }

                field.setAccessible(true);
                field.set(dst, field.get(src));
                dst.signatures = null; // 等待替换
            } catch (Exception e) {
                // 忽略极少数无法赋值的静态常量
                e.printStackTrace();
            }
        }
    }

    /**
     * 替换系统签名
     */
    public boolean hookSignature() {
        if (sHooked)
            return true;
        try {
            Log.d(TAG, "Hook system signature ...");
            PackageManager pm = context.getPackageManager();
            Log.d(TAG, "Type of PackageMagager: " + pm.getClass().getCanonicalName());

            Class<?> pmClazz = pm.getClass(); // 实际上是 android.app.ApplicationPackageManager
            Method srcMethod = pmClazz.getMethod("getPackageInfo", String.class, int.class);

            Class<?> thiz = HookSignature.class;
            Method dstMethod = thiz.getMethod("getPackageInfo", String.class, int.class);

            sPmGetPkgInfoHook.setApiLevel(Build.VERSION.SDK_INT);
            boolean isSuccess = sPmGetPkgInfoHook.hookAndBack(srcMethod, dstMethod);

            sHooked = isSuccess;
            if (isSuccess) {
                Log.d(TAG, "Replace system signature success");
            } else {
                Log.e(TAG, "Hook syste signature fail");
            }
            return isSuccess;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Hook PackageManager.getPackageInfo fail", e);
        }
    }

    /**
     * 不需要 Context，不需要实例化，直接通过静态方法启动 Hook
     * 高版本ApplicationPackageManager被限制反射，需要使用双反射技术实现
     * 这里只是个demo就不做演示了
     */
    public static boolean hookSignatureNoContext() {
        if (sHooked)
            return true;

        if (!initWithoutContext()) {
            return false;
        }

        try {
            Log.d(TAG, "Hook system signature ...");

            Class<?> pmClazz = Class.forName("android.app.ApplicationPackageManager");
            Method srcMethod = null;
            try {
                srcMethod = pmClazz.getDeclaredMethod("getPackageInfo", String.class, int.class);
            } catch (NoSuchMethodException e) {
                // 如果某些极个别 Android 5.0 ROM 在子类没写这个方法（抛了异常）
                // 说明它完全复用了父类，此时我们再退回 getMethod
                srcMethod = pmClazz.getMethod("getPackageInfo", String.class, int.class);
            }

            if (srcMethod == null) // 应该不会在获取失败了，如果还是失败，那就看看rom代码吧
                return false;

            String declaringClassName = srcMethod.getDeclaringClass().getName();
            Log.d(TAG, "Hook system signature class: " + declaringClassName);

            Class<?> thiz = HookSignature.class;
            Method dstMethod = thiz.getMethod("getPackageInfo", String.class, int.class);

            int apilevel = Build.VERSION.SDK_INT;
            sPmGetPkgInfoHook.setApiLevel(apilevel);

            boolean isSuccess = sPmGetPkgInfoHook.hookAndBack(srcMethod, dstMethod);
            sHooked = isSuccess;

            if (isSuccess) {
                Log.d(TAG, "Replace system signature success");
            } else {
                Log.e(TAG, "Hook syste signature fail");
            }

            return isSuccess;
        } catch (Exception e) {
            Log.e(TAG, "Replace system signature fail, get execption!");
            e.printStackTrace();
        }
        return false;
    }
}