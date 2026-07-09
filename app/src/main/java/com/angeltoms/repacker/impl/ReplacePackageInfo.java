/*
 * Copyright AngelToms
 * SPDX-License-Identifier: Apache-2.0
 */

package com.angeltoms.repacker.impl;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.lang.reflect.Field;


/**
 * @Atuthor: AngelToms
 * 获取apk完整安装路径的方式：
 * pm.getPackageArchiveInfo(String archiveFilePath, int flags) 可以解析未安装的完整apk返回一个PackageInfo
 * 这里不需要替换或者hook这个api的原因是，archiveFilePath是必须通过以下api获取：
 * Context.getPackageResourcePath
 * Context.getPackageCodePath
 * Context.getApplication得到application.sourceDir或者publicSourceDir
 * pm.getPackageInfo获取的PackagInfo.application.sourceDir或者publicSourceDir（签名替换中实现）
 * 以上这些均由此类实现替换，但必须时机最早
 * 其他的可以忽略了，比较偏门的实现获取apk安装路径的比较少
 * @Version: 0.1
 */

public class ReplacePackageInfo {

    /**
     * 替换包名，都是组合使用，很多我们引入的第三方模块会校验包名
     * @param context
     * @param pkgName
     */
    public static void replacePkgName(Context context, String pkgName) {
        try {
            // 拿到当前 ContextImpl 里的 mPackageInfo
            Field mPackageInfoField = context.getClass().getDeclaredField("mPackageInfo");
            mPackageInfoField.setAccessible(true);
            Object mPackageInfo = mPackageInfoField.get(context); // 这是一个 LoadedApk 对象

            // 直接修改LoadedApk.mPackageName 字段
            Field mPackageNameField = mPackageInfo.getClass().getDeclaredField("mPackageName");
            mPackageNameField.setAccessible(true);
            mPackageNameField.set(mPackageInfo, pkgName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 替换资源路径，通常有些apk或者sdk往资源目录下放一个伪图片，里面存放一些证书、字节码等
     * 我们对其重构后，如果引用模块硬编码或者强绑定了资源路径，我们需要通过hook替换
     * Asset path
     * 通常mResDir = aInfo.uid == myUid ? aInfo.sourceDir : aInfo.publicSourceDir;
     * 也就是说mResDir大概率和mResDir相同
     * 因此调用replaceApplicationInfo时直接调用replacePkgResPath，完成mResDir的替换
     * 当然你也可以单独调用replacePkgResPath
     * @param context
     * @param resDir
     */
    public static void replacePkgResPath(Context context, String resDir) {
        try {
            // 拿到当前 ContextImpl 里的 mPackageInfo
            Field mPackageInfoField = context.getClass().getDeclaredField("mPackageInfo");
            mPackageInfoField.setAccessible(true);
            Object mPackageInfo = mPackageInfoField.get(context); // 这是一个 LoadedApk 对象

            Field mResDirField = mPackageInfo.getClass().getDeclaredField("mResDir");
            mResDirField.setAccessible(true);
            mResDirField.set(mPackageInfo, resDir);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 替换安装包路径，替换安装包路径是我们构造一个伪包，原包已经安装，但是替换伪包的安装路径指向原包
     * 这样我们引入模块的一些验证拿到数据是原包的，可以绕过验证
     * 通常情况下LoadedApk.mAppDir = ApplicationInfo.sourceDir;
     * 因此调用replaceApplicationInfo时直接内部调用replacePkgCodePath，完成mAppDir的替换
     * 当然你也可以单独调用replacePkgCodePath
     * @param context
     * @param appDir
     */
    public static void replacePkgCodePath(Context context, String appDir) {
        try {
            // 拿到当前 ContextImpl 里的 mPackageInfo
            Field mPackageInfoField = context.getClass().getDeclaredField("mPackageInfo");
            mPackageInfoField.setAccessible(true);
            Object mPackageInfo = mPackageInfoField.get(context); // 这是一个 LoadedApk 对象

            Field mAppDirField = mPackageInfo.getClass().getDeclaredField("mAppDir");
            mAppDirField.setAccessible(true);
            mAppDirField.set(mPackageInfo, appDir);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 同替换安装包，只适合通过下面代码方式获取ApplicationInfo的场景
     * @param context
     * @param sourceDir
     */
    public static void replaceAinfoSourceDir(Context context, String sourceDir) {
        try {
            // 拿到当前 ContextImpl 里的 mPackageInfo
            Field mPackageInfoField = context.getClass().getDeclaredField("mPackageInfo");
            mPackageInfoField.setAccessible(true);
            Object mPackageInfo = mPackageInfoField.get(context); // 这是一个 LoadedApk 对象

            // 直接修改LoadedApk.mPackageName 字段
            Field mApplicationInfoField = mPackageInfo.getClass().getDeclaredField("mApplicationInfo");
            mApplicationInfoField.setAccessible(true);

            ApplicationInfo ai = (ApplicationInfo) mApplicationInfoField.get(mPackageInfo);
            ApplicationInfo newAi = new ApplicationInfo(ai);
            newAi.sourceDir = sourceDir; // 代码基础路径
            newAi.publicSourceDir = sourceDir;

            mApplicationInfoField.set(mPackageInfo, newAi);

            replacePkgCodePath(context, sourceDir);
            replacePkgResPath(context, sourceDir);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
