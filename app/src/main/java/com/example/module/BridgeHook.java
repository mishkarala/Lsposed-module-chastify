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
 * {@code "android"}) and makes {@code DevicePolicyManagerService} report the
 * Chastify app as device/profile owner. This covers two things:</p>
 *
 * <ul>
 *   <li>the app's own self-check — {@code isDeviceOwnerApp(packageName)} — so
 *       Chastify believes it is device owner;</li>
 *   <li>the internal permission checks — {@code isDeviceOwner(CallerIdentity)} /
 *       {@code isProfileOwner(...)} — so its DevicePolicyManager requests are
 *       actually applied by the system.</li>
 * </ul>
 *
 * <p>Scope: System Framework. Chastify itself still has to be enabled as a
 * normal device-admin (Settings &rarr; device admin apps); this module only
 * fakes the elevation from admin to owner.</p>
 *
 * <p>These checks are internal, non-SDK and version-specific, so the methods are
 * located reflectively at runtime instead of being referenced at compile time.</p>
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
            // Covers isDeviceOwnerApp / isProfileOwnerApp (the app's self-check,
            // String packageName) and isDeviceOwner / isProfileOwner (internal
            // permission checks, CallerIdentity or ComponentName).
            if (!name.startsWith("isDeviceOwner") && !name.startsWith("isProfileOwner")) {
                continue;
            }
            if (!returnsBoolean(m) || !hasIdentifiableCaller(m)) {
                continue;
            }
            try {
                XposedBridge.hookMethod(m, OWNER_HOOK);
                hooked++;
                XposedBridge.log(TAG + ": hooked " + name + "(" + paramNames(m) + ")");
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": failed to hook " + name + ": " + t);
            }
        }
        XposedBridge.log(TAG + ": installed " + hooked
                + " owner-check hook(s) for " + CHASTIFY_PKG);
    }

    private static boolean returnsBoolean(Method m) {
        final Class<?> r = m.getReturnType();
        return r == boolean.class || r == Boolean.class;
    }

    private static boolean hasIdentifiableCaller(Method m) {
        for (Class<?> p : m.getParameterTypes()) {
            if (p == String.class || p == ComponentName.class) {
                return true;
            }
            if (CALLER_IDENTITY_CLASS.equals(p.getName())) {
                return true;
            }
        }
        return false;
    }

    private static String paramNames(Method m) {
        final StringBuilder sb = new StringBuilder();
        final Class<?>[] ps = m.getParameterTypes();
        for (int i = 0; i < ps.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(ps[i].getSimpleName());
        }
        return sb.toString();
    }

    /**
     * Forces an ownership check to return {@code true} whenever the subject of
     * that particular call is Chastify (matched by package name, admin component,
     * or caller identity); otherwise lets the original run, so no other app or
     * system component is affected.
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
                if (arg instanceof String) {
                    if (CHASTIFY_PKG.equals(arg)) {
                        force(param);
                        return;
                    }
                } else if (arg instanceof ComponentName) {
                    if (CHASTIFY_PKG.equals(((ComponentName) arg).getPackageName())) {
                        force(param);
                        return;
                    }
                } else if (CALLER_IDENTITY_CLASS.equals(arg.getClass().getName())) {
                    if (CHASTIFY_PKG.equals(callerPackage(arg))) {
                        force(param);
                        return;
                    }
                }
            }
        }

        private void force(MethodHookParam param) {
            param.setResult(Boolean.TRUE);
            XposedBridge.log(TAG + ": forced true -> "
                    + ((Method) param.method).getName());
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
