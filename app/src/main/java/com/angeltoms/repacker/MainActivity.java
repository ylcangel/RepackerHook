package com.angeltoms.repacker;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Log;

import com.angeltoms.repacker.impl.HookSignature;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final String DEFAULT_SIGNATRUE = "3082036f30820257a0030201020204254c2cc6300d06092a864886f70d01010b05003067310f300d060355040613063130303031303110300e060355040813074265694a696e673110300e060355040713074265694a696e67310d300b060355040a130454657374310d300b060355040b1304546573743112301006035504031309416e67656c546f6d733020170d3236303630353033303631315a180f32303531303533303033303631315a3067310f300d060355040613063130303031303110300e060355040813074265694a696e673110300e060355040713074265694a696e67310d300b060355040a130454657374310d300b060355040b1304546573743112301006035504031309416e67656c546f6d7330820122300d06092a864886f70d01010105000382010f003082010a0282010100bea32bdaeccc32e19d936392dc9a95e158ba5fab53efbfb6d8e412628caca4f7a47497be3ea591eaa2f1cb5801780ae1d8336f3fe75df69a5af7c1ff8c22576bbbf12f17b158891e44f0defc620061c4c048048b9c74f66603097b07edf24200809bde018bd5c6ac71d7b8fdd8b715908101c910ca8486a28ad96d4b247a76d6226ec8533761b0284ff6b8d77d10aaed715e6900a40564cd45e7658824d7ed7c0ef2b13ba0a3cc9459a64d3b6be807afe7e33991d386db66bb33522ca39883c209dcf33a26dcfa83583829258097db5e3c35744f41d850484cb3ace46a8773a30375b2f44d8f6317136c52354b4157c8b1b3ccc52508d981dc7cdab9008848850203010001a321301f301d0603551d0e041604145a09edf169add294d4e83b9ad1b2b32a79036b9e300d06092a864886f70d01010b0500038201010054f533094230e0602758856e02421b3b6ff250d491cd94be6c8f73da4c530524ed7776559b39d68a69ffbea8c29f2eb62b47c61cb3789f8b0e39cb28b0065c855711201d86712880195d93824bf949e8b754c1183347492be5ab93c8cf4669e3bcbdadeb5b5d5f0ebf9af9d2e5ce66b8c05f9418f558f0692b54b0e258cbf419765ade6c99901ae99f58b291e8ae0caffd9abff94fc96f5891cb0b96aa3f6e6f4d22baaee3163ce9c237ed97d0a386827c866f4028307556b8d48014514e99dc2c67b4412edd8d3b99037e25775887999f048b23423c156f2658d42c215e656cda52a28bb0a12c599a15deaeb363a625fa909dd0bb77e209dac0ab36cbb09e48";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        try {
//            PackageManager pm = this.getPackageManager();
//            Class<?> pmClass = pm.getClass();
//            Method getPkginfoMeth = pmClass.getMethod("getPackageInfo", String.class, int.class);
//
//            PackageInfo pkgInfo = (PackageInfo)getPkginfoMeth.invoke(pm, getPackageName(), PackageManager.GET_SIGNATURES);
//
//            signatures = pkgInfo.signatures;
//            Log.i(TAG, "signatrue: " + signatures[0].toCharsString());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        Log.i(TAG, "## package name : " + getPackageName());
        Log.i(TAG, "## package resource path : " + getPackageResourcePath());
        Log.i(TAG, "## package code path : " + getPackageCodePath());
        Log.i(TAG, "## application sourceDir : " + getApplicationInfo().sourceDir);
        Log.i(TAG, "## application public sourceDir : " + getApplicationInfo().publicSourceDir);


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

}
