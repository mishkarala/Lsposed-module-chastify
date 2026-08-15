package de.robv.android.xposed;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public final class XposedInterface {
    public interface IXposedHookLoadPackage {
        void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable;
    }

    public static class XC_LoadPackage {
        public static class LoadPackageParam {
            public String packageName;
            public ClassLoader classLoader;
            public String processName;
            public android.content.pm.ApplicationInfo appInfo;
        }
    }

    public static abstract class XC_MethodHook {
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

        public static class MethodHookParam {
            public Object thisObject;
            public Object[] args;
            private Object result;
            private Throwable throwable;

            public Object getResult() { return result; }
            public void setResult(Object result) { this.result = result; }
            public Throwable getThrowable() { return throwable; }
            public void setThrowable(Throwable th) { this.throwable = th; }
            public Object invokeOriginal() throws Throwable { return null; }
        }
    }

    public static final class XposedHelpers {
        public static void findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {}
        public static void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {}
        public static Object callMethod(Object obj, String methodName, Object... args) { return null; }
    }
}
