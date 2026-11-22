// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 hangothanhh

package com.dex120.s22.xposed;

import android.view.Display;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class VirtualDisplaySyncHook {
    private static final int TYPE_EXTERNAL = 2;
    private static final int DISPLAY_DEVICE_EVENT_CHANGED = 2;

    private static volatile float sLastExternalHz = 0f;
    private static volatile WeakReference<Object> sDexVirtualDevice = new WeakReference<>(null);
    private static final ThreadLocal<Boolean> sInGetInfo = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private VirtualDisplaySyncHook() {}

    public static boolean install(ClassLoader cl) {
        boolean anyHooked = false;
        try {
            Class<?> dmsClass = XposedHelpers.findClassIfExists(
                    "com.android.server.display.DisplayManagerService", cl);
            if (dmsClass != null) {
                int cfgCount = XposedBridge.hookAllMethods(
                        dmsClass, "configureDisplayLocked", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                handleConfigureDisplayAfter(param, cl);
                            }
                        }).size();
                if (cfgCount > 0) {
                    anyHooked = true;
                    Config.log("VirtualDisplaySyncHook: hooked DisplayManagerService.configureDisplayLocked ("
                            + cfgCount + ")");
                }
            }

            Class<?> vddClass = XposedHelpers.findClassIfExists(
                    "com.android.server.display.VirtualDisplayAdapter$VirtualDisplayDevice", cl);
            if (vddClass != null) {
                Config.dumpMethodsOnce(vddClass, "VirtualDisplayDevice");

                XposedBridge.hookAllConstructors(vddClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        trackAndSyncIfDesktop(param.thisObject, cl, false);
                    }
                });

                int infoCount = XposedBridge.hookAllMethods(
                        vddClass, "getDisplayDeviceInfoLocked", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                sInGetInfo.set(Boolean.TRUE);
                                trackAndSyncIfDesktop(param.thisObject, cl, false);
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                try {
                                    patchReturnedDisplayDeviceInfo(param.thisObject, param.getResult(), cl);
                                } finally {
                                    sInGetInfo.set(Boolean.FALSE);
                                }
                            }
                        }).size();

                XposedBridge.hookAllMethods(
                        vddClass, "resizeLocked", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                trackAndSyncIfDesktop(param.thisObject, cl, false);
                            }
                        });

                if (infoCount > 0) {
                    anyHooked = true;
                    Config.log("VirtualDisplaySyncHook: hooked VirtualDisplayDevice.getDisplayDeviceInfoLocked");
                }
            }
        } catch (Throwable t) {
            Config.logError("VirtualDisplaySyncHook install failed", t);
        }
        return anyHooked;
    }

    private static void handleConfigureDisplayAfter(
            XC_MethodHook.MethodHookParam param, ClassLoader cl) {
        try {
            if (param.args == null) return;
            Object displayDevice = null;
            for (Object arg : param.args) {
                if (arg != null && arg.getClass().getName().contains("DisplayDevice")) {
                    displayDevice = arg;
                    break;
                }
            }
            if (displayDevice == null) return;

            Object info = XposedHelpers.callMethod(displayDevice, "getDisplayDeviceInfoLocked");
            if (info == null) return;

            int type = Config.getIntFieldSafe(info, "type", -1);
            if (type != TYPE_EXTERNAL) {
                return;
            }

            float activeHz = resolveActiveModeHz(info);
            if (activeHz > 0f) {
                boolean changed = Math.abs(sLastExternalHz - activeHz) > 0.5f;
                sLastExternalHz = activeHz;
                if (changed) {
                    Config.log("External DisplayDevice configured at " + activeHz + " Hz");
                }
                Object dexVd = sDexVirtualDevice.get();
                if (dexVd != null && Config.isSyncVirtual()) {
                    trackAndSyncIfDesktop(dexVd, cl, true);
                }
            }
        } catch (Throwable t) {
            Config.logError("Error in configureDisplayLocked hook", t);
        }
    }

    private static float resolveActiveModeHz(Object displayDeviceInfo) {
        int modeId = Config.getIntFieldSafe(displayDeviceInfo, "modeId", -1);
        Object modesObj = Config.getFieldSafe(displayDeviceInfo, "supportedModes");
        if (modesObj instanceof Display.Mode[]) {
            Display.Mode[] modes = (Display.Mode[]) modesObj;
            for (Display.Mode m : modes) {
                if (m != null && m.getModeId() == modeId) {
                    return m.getRefreshRate();
                }
            }
            if (modes.length > 0 && modes[0] != null) {
                return modes[0].getRefreshRate();
            }
        }
        return Config.getFloatFieldSafe(displayDeviceInfo, "renderFrameRate", 0f);
    }

    private static boolean isDesktopVirtualDevice(Object vdd) {
        if (vdd == null) return false;
        Object nameObj = Config.getFieldSafe(vdd, "mName");
        if ("Desktop".equals(nameObj)) {
            return true;
        }
        Object uniqueIdObj = Config.getFieldSafe(vdd, "mUniqueId");
        return (uniqueIdObj instanceof String) && ((String) uniqueIdObj).contains(",Desktop,");
    }

    private static float computeDesiredVirtualHz() {
        if (!Config.isSyncVirtual()) {
            return 0f;
        }
        float maxCap = (float) Config.getTargetHz();
        if (Config.isForceVirtual120()) {
            return maxCap;
        }
        if (sLastExternalHz > 60.5f) {
            return Math.min(sLastExternalHz, maxCap);
        }
        return 0f;
    }

    private static void trackAndSyncIfDesktop(Object vdd, ClassLoader cl, boolean notifyAdapter) {
        try {
            if (!isDesktopVirtualDevice(vdd)) {
                return;
            }
            sDexVirtualDevice = new WeakReference<>(vdd);

            float targetHz = computeDesiredVirtualHz();
            if (targetHz <= 60.5f) {
                return;
            }

            Object modeObj = Config.getFieldSafe(vdd, "mMode");
            Display.Mode currentMode = (modeObj instanceof Display.Mode) ? (Display.Mode) modeObj : null;
            int width = Config.getIntFieldSafe(vdd, "mWidth",
                    currentMode != null ? currentMode.getPhysicalWidth() : 0);
            int height = Config.getIntFieldSafe(vdd, "mHeight",
                    currentMode != null ? currentMode.getPhysicalHeight() : 0);
            if (width <= 0 || height <= 0) {
                return;
            }

            if (currentMode != null && Math.abs(currentMode.getRefreshRate() - targetHz) <= 0.5f) {
                return;
            }

            Display.Mode newMode = createDisplayMode(width, height, targetHz, cl);
            if (newMode == null) {
                return;
            }

            Config.setFieldSafe(vdd, "mMode", newMode);
            Config.setFieldSafe(vdd, "mRequestedRefreshRate", Float.valueOf(targetHz));
            Config.setFieldSafe(vdd, "mInfo", null);
            Config.log("Synced VirtualDisplay Desktop mMode -> "
                    + width + "x" + height + "@" + targetHz + "Hz");

            if (notifyAdapter && !sInGetInfo.get()) {
                Object adapter = Config.getFieldSafe(vdd, "this$0");
                if (adapter != null) {
                    XposedHelpers.callMethod(
                            adapter, "sendDisplayDeviceEventLocked", vdd, DISPLAY_DEVICE_EVENT_CHANGED);
                    XposedHelpers.callMethod(adapter, "sendTraversalRequestLocked");
                }
            }
        } catch (Throwable t) {
            Config.logError("Error syncing VirtualDisplayDevice", t);
        }
    }

    private static void patchReturnedDisplayDeviceInfo(
            Object vdd, Object info, ClassLoader cl) {
        try {
            if (info == null || !isDesktopVirtualDevice(vdd)) {
                return;
            }
            float targetHz = computeDesiredVirtualHz();
            if (targetHz <= 60.5f) {
                return;
            }

            Object modesObj = Config.getFieldSafe(info, "supportedModes");
            boolean needsPatch = false;
            if (modesObj instanceof Display.Mode[] && ((Display.Mode[]) modesObj).length > 0) {
                Display.Mode first = ((Display.Mode[]) modesObj)[0];
                if (first == null || Math.abs(first.getRefreshRate() - targetHz) > 0.5f) {
                    needsPatch = true;
                }
            } else {
                needsPatch = true;
            }

            if (needsPatch) {
                Object mModeObj = Config.getFieldSafe(vdd, "mMode");
                Display.Mode mMode = (mModeObj instanceof Display.Mode) ? (Display.Mode) mModeObj : null;
                if (mMode == null || Math.abs(mMode.getRefreshRate() - targetHz) > 0.5f) {
                    int w = Config.getIntFieldSafe(info, "width", 1920);
                    int h = Config.getIntFieldSafe(info, "height", 1080);
                    mMode = createDisplayMode(w, h, targetHz, cl);
                    if (mMode != null) {
                        Config.setFieldSafe(vdd, "mMode", mMode);
                    }
                }
                if (mMode != null) {
                    Config.setFieldSafe(info, "supportedModes", new Display.Mode[] { mMode });
                    Config.setFieldSafe(info, "modeId", Integer.valueOf(mMode.getModeId()));
                    Config.setFieldSafe(info, "defaultModeId", Integer.valueOf(mMode.getModeId()));
                }
            }

            // Defensive update if ROM has renderFrameRate field on DisplayDeviceInfo.
            float currentRender = Config.getFloatFieldSafe(info, "renderFrameRate", -1f);
            if (currentRender > 0f && Math.abs(currentRender - targetHz) > 0.5f) {
                Config.setFieldSafe(info, "renderFrameRate", Float.valueOf(targetHz));
            }
        } catch (Throwable t) {
            Config.logError("Error patching VirtualDisplayDeviceInfo", t);
        }
    }

    private static Display.Mode createDisplayMode(
            int width, int height, float refreshRate, ClassLoader cl) {
        try {
            Class<?> displayAdapterClass = XposedHelpers.findClassIfExists(
                    "com.android.server.display.DisplayAdapter", cl);
            if (displayAdapterClass != null) {
                try {
                    Method m = displayAdapterClass.getDeclaredMethod(
                            "createMode", int.class, int.class, float.class);
                    m.setAccessible(true);
                    Object mode = m.invoke(null, width, height, refreshRate);
                    if (mode instanceof Display.Mode) {
                        return (Display.Mode) mode;
                    }
                } catch (NoSuchMethodException ignored) {}
            }

            // Fallback: allocate modeId via NEXT_DISPLAY_MODE_ID or static counter and invoke Display.Mode ctor.
            int modeId = 9001;
            if (displayAdapterClass != null) {
                Object nextId = Config.getStaticFieldSafe(displayAdapterClass, "NEXT_DISPLAY_MODE_ID");
                if (nextId instanceof java.util.concurrent.atomic.AtomicInteger) {
                    modeId = ((java.util.concurrent.atomic.AtomicInteger) nextId).getAndIncrement();
                }
            }
            for (Constructor<?> ctor : Display.Mode.class.getDeclaredConstructors()) {
                Class<?>[] p = ctor.getParameterTypes();
                if (p.length == 4 && p[0] == int.class && p[1] == int.class
                        && p[2] == int.class && p[3] == float.class) {
                    ctor.setAccessible(true);
                    return (Display.Mode) ctor.newInstance(modeId, width, height, refreshRate);
                }
            }
        } catch (Throwable t) {
            Config.logError("Failed to create Display.Mode", t);
        }
        return null;
    }
}
