package com.example.module;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class BridgeHook implements XposedInterface.IXposedHookLoadPackage {

    private static final String CHASTIFY_PKG = "net.chastify.app"; 
    private static final String OWNDROID_PKG = "com.bintianqi.owndroid";

    @Override
    public void handleLoadPackage(XposedInterface.XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName.equals(CHASTIFY_PKG)) {
            
            XposedInterface.XposedHelpers.findAndHookMethod(
                "android.app.admin.DevicePolicyManager",
                lpparam.classLoader,
                "isDeviceOwnerApp",
                String.class,
                new XposedInterface.XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String pkg = (String) param.args[0];
                        if (CHASTIFY_PKG.equals(pkg)) {
                            param.setResult(true);
                        }
                    }
                }
            );

            XposedInterface.XposedHelpers.findAndHookMethod(
                "android.app.admin.DevicePolicyManager",
                lpparam.classLoader,
                "setUserRestriction",
                android.content.ComponentName.class,
                String.class,
                String.class,
                new XposedInterface.XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String restrictionKey = (String) param.args[1];
                        param.setResult(null); 

                        Context context = (Context) XposedInterface.XposedHelpers.callMethod(param.thisObject, "getContext");
                        if (context != null) {
                            Intent intent = new Intent("com.owndroid.ACTION_APPLY_RESTRICTION");
                            intent.setPackage(OWNDROID_PKG);
                            intent.putExtra("restriction", restrictionKey);
                            context.sendBroadcast(intent);
                        }
                    }
                }
            );
        }
    }
}
