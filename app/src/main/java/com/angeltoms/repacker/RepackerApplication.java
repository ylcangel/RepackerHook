/*
 * Copyright AngelToms
 * SPDX-License-Identifier: Apache-2.0
 */

package com.angeltoms.repacker;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;

import com.angeltoms.repacker.impl.HookSignature;
import com.angeltoms.repacker.impl.ReplacePackageInfo;

/**
 * @Author: AngelToms
 */

public class RepackerApplication extends Application {

    public final String TAG = "RepackerApplication";

    @Override
    public void onCreate() {
        super.onCreate();
//        Log.i(TAG, ">> package name : " + getPackageName());
//        Log.i(TAG, ">> package resource path : " + getPackageResourcePath());
//        Log.i(TAG, ">> package code path : " + getPackageCodePath());
//        Log.i(TAG, ">> application sourceDir : " + getApplicationInfo().sourceDir);
//        Log.i(TAG, ">> application public sourceDir : " + getApplicationInfo().publicSourceDir);
    }

    // 插入时机尽量早，即使是加壳程序也要在调用super.attachBaseContext前面插入
    // 为简单操作，我们除打印部分代码，全部移入一个新的static的method里，然后把调用插入attachBaseContext
    // printPkgInfo(base);
    ////////////////////////////////////////////////////////////////////////////////////////////
    // 均是对抗签名，防止从安装包解析签名，仅用于对抗加固
    // ReplacePackageInfo.replacePkgName(base, "com.fuck.you");
    // ReplacePackageInfo.replaceAinfoSourceDir(base, "/data/app/com.test.example/base.apk");
    ////////////////////////////////////////////////////////////////////////////////////////////
    // printPkgInfo(base);
    ////////////////////////////////////////////////////////////////////////////////////////////

    // printSignature(base);
    // HookSignature.hookSignatureNoContext();
    ///////////////////////////////////////////////
    // context版
    // HookSignature hs = new HookSignature(base);
    // hs.hookSignature();
    ///////////////////////////////////////////////
    // printSignature(base);
    ////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void attachBaseContext(Context base) {
        // inject code
        super.attachBaseContext(base);
    }

    public void printSignature(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;

            Signature[] signatures = packageInfo.signatures;
            Log.i(TAG, "signatrue: " + signatures[0].toCharsString());
            Log.i(TAG, "ApplicationInfo sourceDir: " + applicationInfo.sourceDir + ", publicSourceDir: " +
                    applicationInfo.publicSourceDir + ", dataDir: " + applicationInfo.dataDir);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void printPkgInfo(Context context) {
        Log.i(TAG, "!! package name : " + context.getPackageName());
        Log.i(TAG, "!! package resource path : " + context.getPackageResourcePath());
        Log.i(TAG, "!! package code path : " + context.getPackageCodePath());
        Log.i(TAG, "!! application sourceDir : " + context.getApplicationInfo().sourceDir);
        Log.i(TAG, "!! application public sourceDir : " + context.getApplicationInfo().publicSourceDir);

    }
}
