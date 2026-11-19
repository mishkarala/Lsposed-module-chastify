package com.example.module;

import android.content.ComponentName;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Chastify device-owner bridge (classic Xposed API).
 *
 * <p>Loads into {@code system_server} (which Xposed exposes as the package
 * {@code "android"}) and makes {@code DevicePolicyManagerService} treat the
 * Chastify app as device/profile owner for its own calls. This lets Chastify's
 * DevicePolicyManager requests — e.g. {@code addUserRestriction} — actually be
 * applied by the system, without a second app (Owndroid) and without really
 * provisioning Chastify as device owner.</p>
 *
 * <p>Scope: System Framework. Chastify itself still has to be enabled as a
 * normal device-admin (Settings &rarr; device admin apps); this module only
 * fakes the elevation from admin to owner.</p>
 *
 * <p>The ownership checks are internal, non-SDK and version-specific, so the
 * methods are located reflectively at runtime instead of being referenced at
 * compile time.</p>
 */
public class BridgeHook implements IXposedHookLoadPackage {

    private static final String TAG = "ChastifyBridge";

    private static final String CHASTIFY_PKG = "net.chastify.app";

    private static final String DPMS_CLASS =
            "com.android.server.devicepolicy.DevicePolicyManagerService";
    private static final String CALLER_IDENTITY_CLASS =
            "com.android.server.devicepolicy.CallerIdentity";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        // DevicePolicyManagerService lives in system_server, loaded as "android".
        if (!"android".equals(lpparam.packageName)) {
            return;
        }

        final Class<?> dpms;
        try {
            dpms = lpparam.classLoader.loadClass(DPMS_CLASS);
        } catch (ClassNotFoundException e) {
            XposedBridge.log(TAG + ": DevicePolicyManagerService not found, aborting");
            return;
        }

        int hooked = 0;
        for (Method m : dpms.getDeclaredMethods()) {
            final String name = m.getName();
            if (!name.equals("isDeviceOwner") && !name.equals("isProfileOwner")) {
                continue;
            }
            // Only hook overloads whose arguments let us identify the caller:
            // a CallerIdentity, or a ComponentName (the app's admin component).
            if (!hasIdentifiableCaller(m)) {
                continue;
            }
            try {
                XposedBridge.hookMethod(m, OWNER_HOOK);
                hooked++;
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": failed to hook " + name + ": " + t);
            }
        }
        XposedBridge.log(TAG + ": installed " + hooked
                + " owner-check hook(s) for " + CHASTIFY_PKG);
    }

    private static boolean hasIdentifiableCaller(Method m) {
        for (Class<?> p : m.getParameterTypes()) {
            if (p == ComponentName.class) {
                return true;
            }
            if (CALLER_IDENTITY_CLASS.equals(p.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Forces an ownership check to return {@code true} whenever the caller in
     * that particular call is Chastify; otherwise lets the original run, so no
     * other app or system component is affected.
     */
    private static final XC_MethodHook OWNER_HOOK = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            if (param.args == null) {
                return;
            }
            for (Object arg : param.args) {
                if (arg == null) {
                    continue;
                }
                if (arg instanceof ComponentName) {
                    if (CHASTIFY_PKG.equals(((ComponentName) arg).getPackageName())) {
                        param.setResult(Boolean.TRUE);
                        return;
                    }
                } else if (CALLER_IDENTITY_CLASS.equals(arg.getClass().getName())) {
                    if (CHASTIFY_PKG.equals(callerPackage(arg))) {
                        param.setResult(Boolean.TRUE);
                        return;
                    }
                }
            }
        }
    };

    private static String callerPackage(Object callerIdentity) {
        try {
            final Method gp = callerIdentity.getClass().getMethod("getPackageName");
            final Object r = gp.invoke(callerIdentity);
            return r == null ? null : r.toString();
        } catch (Throwable t) {
            return null;
        }
    }
}
