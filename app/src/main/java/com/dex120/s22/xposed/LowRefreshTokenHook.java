// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 hangothanhh

package com.dex120.s22.xposed;

import android.content.Context;
import android.os.Binder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class LowRefreshTokenHook {
    private static final Binder DUMMY_BINDER = new Binder();
    private static final ConcurrentHashMap<Class<?>, Object> sDummyTokens = new ConcurrentHashMap<>();

    private LowRefreshTokenHook() {}

    public static boolean install(ClassLoader cl) {
        boolean anyHooked = false;
        try {
            String[] targetClasses = new String[] {
                    "com.android.server.display.DisplayManagerService$BinderService",
                    "com.android.server.display.DisplayManagerService",
                    "com.android.server.display.DisplayManagerService$LocalService"
            };

            for (String className : targetClasses) {
                Class<?> clazz = XposedHelpers.findClassIfExists(className, cl);
                if (clazz == null) {
                    continue;
                }
                if (className.endsWith("DisplayManagerService")) {
                    captureContextFromConstructors(clazz);
                }

                int lowCount = XposedBridge.hookAllMethods(
                        clazz, "acquireLowRefreshRateToken", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                handleTokenRequest(param, cl, false);
                            }
                        }).size();

                int maxCount = XposedBridge.hookAllMethods(
                        clazz, "acquireRefreshRateMaxLimitToken", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                handleTokenRequest(param, cl, true);
                            }
                        }).size();

                if (lowCount > 0 || maxCount > 0) {
                    anyHooked = true;
                    Config.log("LowRefreshTokenHook: " + className
                            + " (low=" + lowCount + ", maxLimit=" + maxCount + ")");
                }
            }
        } catch (Throwable t) {
            Config.logError("LowRefreshTokenHook install failed", t);
        }
        return anyHooked;
    }

    private static void captureContextFromConstructors(Class<?> dmsClass) {
        try {
            XposedBridge.hookAllConstructors(dmsClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof Context) {
                        Config.setSystemContext((Context) param.args[0]);
                    }
                }
            });
        } catch (Throwable t) {
            Config.logError("Failed to hook DisplayManagerService constructors", t);
        }
    }

    private static void handleTokenRequest(
            XC_MethodHook.MethodHookParam param, ClassLoader cl, boolean isMaxLimitToken) {
        try {
            if (!Config.isBlockToken()) {
                return;
            }

            String tag = findTagArg(param.args);
            int maxLimitHz = findMaxLimitArg(param.args);

            if (isMaxLimitToken && maxLimitHz > 0 && maxLimitHz > 60) {
                return;
            }

            if (!isDexCaller(tag) && !DisplayModeHook.hasActiveExternalDisplay()) {
                Config.logOnce("unblocked_token_" + isMaxLimitToken + "_" + tag,
                        "Observed non-DeX token ("
                                + (isMaxLimitToken ? "maxLimit=" + maxLimitHz : "lowRefresh")
                                + "): tag=" + tag);
                return;
            }

            Object dummy = getOrCreateDummyToken(cl, param.method, isMaxLimitToken);
            param.setResult(dummy);
            Config.log("Blocked DeX 60Hz token ("
                    + (isMaxLimitToken ? "maxLimit=" + maxLimitHz : "lowRefresh")
                    + "): tag=" + tag
                    + ", extActive=" + DisplayModeHook.hasActiveExternalDisplay());
            SdhmsDfsHook.scheduleDfsReset("token:" + tag);
        } catch (Throwable t) {
            Config.logError("Error in LowRefreshTokenHook", t);
        }
    }

    private static String findTagArg(Object[] args) {
        if (args == null) return "";
        for (Object arg : args) {
            if (arg instanceof String) {
                return (String) arg;
            }
        }
        return "";
    }

    private static int findMaxLimitArg(Object[] args) {
        if (args == null) return -1;
        for (Object arg : args) {
            if (arg instanceof Integer) {
                return (Integer) arg;
            }
        }
        return -1;
    }

    private static boolean isDexCaller(String tag) {
        if (tag != null) {
            String lower = tag.toLowerCase(Locale.ROOT);
            if (lower.contains("dex")
                    || lower.contains("desktop")
                    || lower.contains("dms")
                    || lower.contains("hardwaremanager")
                    || lower.contains("hdmi")
                    || lower.contains("displayport")
                    || lower.contains("touchpad")
                    || lower.contains("external")
                    || lower.contains("dual")
                    || lower.contains("mirror")
                    || lower.contains("smartview")) {
                return true;
            }
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement frame : stack) {
            if (frame.getClassName().contains("com.android.server.desktopmode")) {
                return true;
            }
        }
        return false;
    }

    private static Object getOrCreateDummyToken(
            ClassLoader cl, Member method, boolean isMaxLimitToken) {
        try {
            Class<?> returnType = (method instanceof Method) ? ((Method) method).getReturnType() : null;
            Class<?> iface = returnType;
            if (iface == null || !iface.isInterface()) {
                iface = XposedHelpers.findClassIfExists(
                        isMaxLimitToken
                                ? "com.samsung.android.hardware.display.IRefreshRateMaxLimitToken"
                                : "com.samsung.android.hardware.display.ILowRefreshRateToken",
                        cl);
            }
            if (iface == null || !iface.isInterface()) {
                iface = XposedHelpers.findClassIfExists(
                        "com.samsung.android.hardware.display.IRefreshRateToken", cl);
            }
            if (iface != null && iface.isInterface()) {
                Object existing = sDummyTokens.get(iface);
                if (existing != null) {
                    return existing;
                }
                final String proxyLabel = "DeX120HzDummy(" + iface.getSimpleName() + ")";
                Object proxy = Proxy.newProxyInstance(
                        iface.getClassLoader(),
                        new Class<?>[] { iface },
                        new InvocationHandler() {
                            @Override
                            public Object invoke(Object p, Method m, Object[] args) {
                                String name = m.getName();
                                if ("release".equals(name)) {
                                    return null;
                                }
                                if ("asBinder".equals(name)) {
                                    return DUMMY_BINDER;
                                }
                                if ("toString".equals(name)) {
                                    return proxyLabel;
                                }
                                if ("hashCode".equals(name)) {
                                    return System.identityHashCode(p);
                                }
                                if ("equals".equals(name)) {
                                    return args != null && args.length == 1 && args[0] == p;
                                }
                                Class<?> ret = m.getReturnType();
                                if (ret == boolean.class) return Boolean.FALSE;
                                if (ret == int.class) return 0;
                                if (ret == long.class) return 0L;
                                if (ret == float.class) return 0f;
                                if (ret == double.class) return 0d;
                                return null;
                            }
                        });
                Object prev = sDummyTokens.putIfAbsent(iface, proxy);
                return prev != null ? prev : proxy;
            }
        } catch (Throwable t) {
            Config.logError("Failed to create dummy refresh rate token proxy", t);
        }
        return null;
    }
}
