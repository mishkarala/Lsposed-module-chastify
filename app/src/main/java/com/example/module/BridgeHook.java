package com.example.module;

import android.content.Context;
import android.content.Intent;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

public class BridgeHook extends XposedModule {

    private static final String CHASTIFY_PKG = "net.chastify.app"; 
    private static final String OWNDROID_PKG = "com.bintianqi.owndroid";

    public BridgeHook(XposedInterface base, ModuleLoadedParam parameters) {
        super(base, parameters);
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam lpparam) {
        if (lpparam.getPackageName().equals(CHASTIFY_PKG)) {
            
            // Хуки для LibXposed 102+
            hookMethod(
                android.app.admin.DevicePolicyManager.class,
                "isDeviceOwnerApp",
                new Class[]{String.class},
                new XposedInterface.HookCallback() {
                    @Override
                    public void after(MethodHookParam param) throws Throwable {
                        String pkg = (String) param.getArg(0);
                        if (CHASTIFY_PKG.equals(pkg)) {
                            param.setResult(true);
                        }
                    }
                }
            );

            hookMethod(
                android.app.admin.DevicePolicyManager.class,
                "setUserRestriction",
                new Class[]{android.content.ComponentName.class, String.class, String.class},
                new XposedInterface.HookCallback() {
                    @Override
                    public void before(MethodHookParam param) throws Throwable {
                        String restrictionKey = (String) param.getArg(1);
                        param.setResult(null); 

                        Context context = (Context) getContextFromObject(param.thisObject());
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

    private Context getContextFromObject(Object obj) {
        try {
            java.lang.reflect.Method method = obj.getClass().getMethod("getContext");
            return (Context) method.invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
