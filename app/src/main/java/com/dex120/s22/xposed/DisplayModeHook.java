package com.dex120.s22.xposed;

import android.util.SparseArray;
import android.view.Display;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class DisplayModeHook {
    private static final int TYPE_EXTERNAL = 2;
    private static final ConcurrentHashMap<Integer, Integer> sDisplayTypes = new ConcurrentHashMap<>();
    private static volatile boolean sExternalDisplayActive = false;

    private DisplayModeHook() {}

    public static boolean hasActiveExternalDisplay() {
        return sExternalDisplayActive;
    }

    public static boolean install(ClassLoader cl) {
        boolean anyHooked = false;
        try {
            hookLogicalDisplayTracking(cl);

            String[] directorNames = new String[] {
                    "com.android.server.display.DisplayModeDirector",
                    "com.android.server.display.mode.DisplayModeDirector"
            };
            for (String directorName : directorNames) {
                Class<?> directorClass = XposedHelpers.findClassIfExists(directorName, cl);
                if (directorClass == null) {
                    continue;
                }
                Config.dumpMethodsOnce(directorClass, "DisplayModeDirector");

                int specsCount = XposedBridge.hookAllMethods(
                        directorClass, "getDesiredDisplayModeSpecs", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                handleDesiredSpecsAfter(param);
                            }
                        }).size();

                int voteCount = XposedBridge.hookAllMethods(
                        directorClass, "updateVoteLocked", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                handleUpdateVoteBefore(param);
                            }
                        }).size();

                hookDisplayObserver(directorName, cl);

                if (specsCount > 0) {
                    anyHooked = true;
                    Config.log("DisplayModeHook: hooked " + directorName
                            + " (specs=" + specsCount + ", vote=" + voteCount + ")");
                }
            }
        } catch (Throwable t) {
            Config.logError("DisplayModeHook install failed", t);
        }
        return anyHooked;
    }

    private static void setExternalActive(boolean active) {
        boolean wasActive = sExternalDisplayActive;
        sExternalDisplayActive = active;
        if (!wasActive && active && Config.isBlockToken()) {
            SdhmsDfsHook.scheduleDfsReset("external_display_active");
        }
    }

    private static void recomputeExternalActive() {
        boolean active = false;
        for (Integer id : sDisplayTypes.keySet()) {
            if (id != null && id > 0) {
                Integer type = sDisplayTypes.get(id);
                if (type != null && type == TYPE_EXTERNAL) {
                    active = true;
                    break;
                }
            }
        }
        setExternalActive(active);
    }

    private static void syncActiveTypesWithSupported(SparseArray<Display.Mode[]> supportedMap) {
        if (supportedMap == null) {
            recomputeExternalActive();
            return;
        }
        boolean active = false;
        for (Integer id : sDisplayTypes.keySet()) {
            if (id == null || id <= 0) {
                continue;
            }
            if (supportedMap.get(id) == null) {
                sDisplayTypes.remove(id);
            } else {
                Integer type = sDisplayTypes.get(id);
                if (type != null && type == TYPE_EXTERNAL) {
                    active = true;
                }
            }
        }
        setExternalActive(active);
    }

    private static void recordDisplayInfo(Object infoObj) {
        if (infoObj == null) return;
        if (!"android.view.DisplayInfo".equals(infoObj.getClass().getName())) {
            return;
        }
        int id = Config.getIntFieldSafe(infoObj, "displayId", -1);
        int type = Config.getIntFieldSafe(infoObj, "type", 0);
        if (id >= 0 && type != 0) {
            sDisplayTypes.put(id, type);
            recomputeExternalActive();
        }
    }

    private static void hookLogicalDisplayTracking(ClassLoader cl) {
        try {
            Class<?> logicalDisplayClass = XposedHelpers.findClassIfExists(
                    "com.android.server.display.LogicalDisplay", cl);
            if (logicalDisplayClass != null) {
                XposedBridge.hookAllMethods(
                        logicalDisplayClass, "getDisplayInfoLocked", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                recordDisplayInfo(param.getResult());
                            }
                        });
            }

            Class<?> realInjectorClass = XposedHelpers.findClassIfExists(
                    "com.android.server.display.DisplayModeDirector$RealInjector", cl);
            if (realInjectorClass == null) {
                realInjectorClass = XposedHelpers.findClassIfExists(
                        "com.android.server.display.mode.DisplayModeDirector$RealInjector", cl);
            }
            if (realInjectorClass != null) {
                XposedBridge.hookAllMethods(
                        realInjectorClass, "getDisplayInfo", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (param.args != null && param.args.length >= 2
                                        && param.args[0] instanceof Integer
                                        && param.args[1] != null) {
                                    int id = (Integer) param.args[0];
                                    int type = Config.getIntFieldSafe(param.args[1], "type", 0);
                                    if (type != 0) {
                                        sDisplayTypes.put(id, type);
                                        recomputeExternalActive();
                                    }
                                }
                            }
                        });
            }
        } catch (Throwable t) {
            Config.logError("LogicalDisplay tracking warning", t);
        }
    }

    private static void hookDisplayObserver(String directorName, ClassLoader cl) {
        try {
            Class<?> observerClass = XposedHelpers.findClassIfExists(
                    directorName + "$DisplayObserver", cl);
            if (observerClass == null) {
                return;
            }
            XposedBridge.hookAllMethods(observerClass, "onDisplayRemoved", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args.length >= 1 && param.args[0] instanceof Integer) {
                        sDisplayTypes.remove((Integer) param.args[0]);
                        recomputeExternalActive();
                    }
                }
            });
            for (Method m : observerClass.getDeclaredMethods()) {
                if (m.getName().contains("updateDisplay") || m.getName().contains("onDisplay")) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.args != null) {
                                for (Object arg : param.args) {
                                    recordDisplayInfo(arg);
                                }
                            }
                        }
                    });
                }
            }
        } catch (Throwable t) {
            Config.logError("DisplayObserver tracking warning", t);
        }
    }

    private static boolean isExternalDisplay(int displayId) {
        if (displayId <= 0) {
            return false;
        }
        Integer type = sDisplayTypes.get(displayId);
        return type != null && type == TYPE_EXTERNAL;
    }

    private static void handleUpdateVoteBefore(XC_MethodHook.MethodHookParam param) {
        try {
            if (!Config.isEnabled() || param.args == null || param.args.length < 3) {
                return;
            }
            if (!(param.args[0] instanceof Integer) || !(param.args[1] instanceof Integer)) {
                return;
            }
            int displayId = (Integer) param.args[0];
            int priority = (Integer) param.args[1];
            Object vote = param.args[2];
            if (vote == null) {
                return;
            }

            Object range = Config.getFieldSafe(vote, "refreshRateRange");
            if (range == null) {
                return;
            }
            float min = Config.getFloatFieldSafe(range, "min", 0f);
            float max = Config.getFloatFieldSafe(range, "max", 0f);
            if (max <= 0f || max > 60.5f) {
                return;
            }

            if (isExternalDisplay(displayId) && Config.isUnlockModes()) {
                Config.log("Ext display " + displayId + " vote capped <=60Hz: priority="
                        + priority + " range=[" + min + ", " + max + "]");
            } else if (sExternalDisplayActive && Config.isBlockToken()
                    && (displayId == 0 || displayId == -1)) {
                Config.logOnce("disp0_cap_" + displayId + "_" + priority,
                        "Observed Display " + displayId + " <=60Hz vote while external active: priority="
                                + priority + " range=[" + min + ", " + max + "]");
            }
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unchecked")
    private static void handleDesiredSpecsAfter(XC_MethodHook.MethodHookParam param) {
        try {
            if (!Config.isEnabled() || param.args == null || param.args.length < 1) {
                return;
            }
            if (!(param.args[0] instanceof Integer)) {
                return;
            }
            int displayId = (Integer) param.args[0];

            Object specs = param.getResult();
            if (specs == null) {
                return;
            }

            Object supportedObj = Config.getFieldSafe(param.thisObject, "mSupportedModesByDisplay");
            if (!(supportedObj instanceof SparseArray)) {
                return;
            }
            SparseArray<Display.Mode[]> supportedMap = (SparseArray<Display.Mode[]>) supportedObj;
            syncActiveTypesWithSupported(supportedMap);

            if (displayId == 0) {
                if (sExternalDisplayActive && Config.isBlockToken()) {
                    protectInternalDisplaySpecs(specs, supportedMap.get(0));
                }
                return;
            }

            if (!isExternalDisplay(displayId) || !Config.isUnlockModes()) {
                return;
            }

            Display.Mode[] modes = supportedMap.get(displayId);
            if (modes == null || modes.length == 0) {
                return;
            }

            Display.Mode currentBase = resolveBaseMode(specs, modes);
            if (currentBase == null) {
                return;
            }

            float targetCap = (float) Config.getTargetHz();
            Display.Mode bestMode = findHighestModeSameRes(modes, currentBase, targetCap);

            if (bestMode.getModeId() != currentBase.getModeId()) {
                Config.setFieldSafe(specs, "baseModeId", Integer.valueOf(bestMode.getModeId()));
                Config.log("Upgraded external display " + displayId + " baseModeId: "
                        + currentBase.getModeId() + " (" + currentBase.getPhysicalWidth() + "x"
                        + currentBase.getPhysicalHeight() + "@"
                        + currentBase.getRefreshRate() + "Hz) -> "
                        + bestMode.getModeId() + " (@" + bestMode.getRefreshRate() + "Hz)");
            }

            float desiredMax = Math.max(bestMode.getRefreshRate(), Math.min(targetCap, 120f));
            applySpecRanges(specs, desiredMax, false, true);
        } catch (Throwable t) {
            Config.logError("Error in DisplayModeHook.getDesiredDisplayModeSpecs", t);
        }
    }

    private static void protectInternalDisplaySpecs(Object specs, Display.Mode[] modes) {
        if (specs == null || modes == null || modes.length == 0) {
            return;
        }
        Display.Mode currentBase = resolveBaseMode(specs, modes);
        if (currentBase == null) {
            return;
        }

        Display.Mode bestMode = findHighestModeSameRes(modes, currentBase, (float) Config.getTargetHz());
        if (bestMode.getRefreshRate() <= 60.5f) {
            return;
        }

        if (bestMode.getModeId() != currentBase.getModeId()) {
            Config.setFieldSafe(specs, "baseModeId", Integer.valueOf(bestMode.getModeId()));
        }

        float rawHz = bestMode.getRefreshRate();
        float lockedHz = (Math.abs(rawHz - Math.round(rawHz)) < 0.1f)
                ? (float) Math.round(rawHz) : rawHz;

        applySpecRanges(specs, lockedHz, true, false);

        Config.logOnce("lock_disp0_" + bestMode.getModeId(),
                "Locked Display 0 to " + currentBase.getPhysicalWidth() + "x"
                        + currentBase.getPhysicalHeight() + "@" + lockedHz
                        + "Hz (modeId=" + bestMode.getModeId() + ") while external display active");
    }

    private static Display.Mode resolveBaseMode(Object specs, Display.Mode[] modes) {
        int baseModeId = Config.getIntFieldSafe(specs, "baseModeId", -1);
        Display.Mode currentBase = findModeById(modes, baseModeId);
        return (currentBase != null) ? currentBase : modes[0];
    }

    private static Display.Mode findHighestModeSameRes(
            Display.Mode[] modes, Display.Mode currentBase, float targetCap) {
        int targetW = currentBase.getPhysicalWidth();
        int targetH = currentBase.getPhysicalHeight();
        Display.Mode bestMode = currentBase;
        for (Display.Mode m : modes) {
            if (m != null && m.getPhysicalWidth() == targetW && m.getPhysicalHeight() == targetH) {
                float hz = m.getRefreshRate();
                if (hz <= targetCap + 1f && hz > bestMode.getRefreshRate() + 0.5f) {
                    bestMode = m;
                }
            }
        }
        return bestMode;
    }

    private static void applySpecRanges(
            Object specs, float targetHz, boolean exactPrimary, boolean logUnlock) {
        // Android 12/13 DesiredDisplayModeSpecs fields:
        Object primaryRange = Config.getFieldSafe(specs, "primaryRefreshRateRange");
        if (exactPrimary) {
            lockRangeExact(primaryRange, targetHz);
        } else {
            unlockRange(primaryRange, targetHz, logUnlock);
        }
        unlockRange(Config.getFieldSafe(specs, "appRequestRefreshRateRange"), targetHz, logUnlock);

        // Android 14+ defensive fallback if ever present:
        Object primary = Config.getFieldSafe(specs, "primary");
        if (primary != null) {
            Object phys = Config.getFieldSafe(primary, "physical");
            Object rend = Config.getFieldSafe(primary, "render");
            if (exactPrimary) {
                lockRangeExact(phys, targetHz);
                lockRangeExact(rend, targetHz);
            } else {
                unlockRange(phys, targetHz, logUnlock);
                unlockRange(rend, targetHz, logUnlock);
            }
        }
        Object appRequest = Config.getFieldSafe(specs, "appRequest");
        if (appRequest != null) {
            unlockRange(Config.getFieldSafe(appRequest, "physical"), targetHz, logUnlock);
            unlockRange(Config.getFieldSafe(appRequest, "render"), targetHz, logUnlock);
        }
    }

    private static Display.Mode findModeById(Display.Mode[] modes, int modeId) {
        for (Display.Mode m : modes) {
            if (m != null && m.getModeId() == modeId) {
                return m;
            }
        }
        return null;
    }

    private static void lockRangeExact(Object range, float lockedHz) {
        if (range == null || lockedHz <= 60.5f) {
            return;
        }
        float min = Config.getFloatFieldSafe(range, "min", 0f);
        float max = Config.getFloatFieldSafe(range, "max", 0f);
        if (Math.abs(min - lockedHz) > 0.5f || Math.abs(max - lockedHz) > 0.5f) {
            Config.setFieldSafe(range, "min", Float.valueOf(lockedHz));
            Config.setFieldSafe(range, "max", Float.valueOf(lockedHz));
        }
    }

    private static void unlockRange(Object range, float desiredMax, boolean logChange) {
        if (range == null || desiredMax <= 60.5f) {
            return;
        }
        float min = Config.getFloatFieldSafe(range, "min", 0f);
        float max = Config.getFloatFieldSafe(range, "max", 0f);
        if (max < desiredMax - 0.5f) {
            Config.setFieldSafe(range, "max", Float.valueOf(desiredMax));
            if (min > desiredMax) {
                Config.setFieldSafe(range, "min", Float.valueOf(0f));
            }
            if (logChange) {
                Config.logOnce("unlock_range_" + System.identityHashCode(range),
                        "Raised external RefreshRateRange max: " + max + " -> " + desiredMax);
            }
        }
    }
}
