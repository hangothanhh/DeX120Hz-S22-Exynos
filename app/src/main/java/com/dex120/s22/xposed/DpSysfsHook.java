package com.dex120.s22.xposed;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class DpSysfsHook {
    private static final String DP_DEX_SUFFIX = "dp_sec/dex";
    private static volatile boolean sSimulatedDexOn = false;

    private DpSysfsHook() {}

    public static boolean install(ClassLoader cl) {
        boolean anyHooked = false;
        try {
            Class<?> utilsClass = XposedHelpers.findClassIfExists(
                    "com.android.server.desktopmode.Utils", cl);
            Class<?> dpStateClass = XposedHelpers.findClassIfExists(
                    "com.android.server.desktopmode.DisplayPortStateManager", cl);
            Class<?> hwManagerClass = XposedHelpers.findClassIfExists(
                    "com.android.server.desktopmode.HardwareManager", cl);

            Config.dumpMethodsOnce(utilsClass, "DesktopModeUtils");
            Config.dumpMethodsOnce(dpStateClass, "DisplayPortStateManager");
            Config.dumpMethodsOnce(hwManagerClass, "DesktopHardwareManager");

            if (utilsClass != null) {
                int writeCount = XposedBridge.hookAllMethods(
                        utilsClass, "writeFile", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                handleWriteFileBefore(param);
                            }
                        }).size();

                int readCount = XposedBridge.hookAllMethods(
                        utilsClass, "readFile", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                handleReadFileAfter(param);
                            }
                        }).size();

                Config.log("DpSysfsHook: hooked Utils.writeFile (" + writeCount
                        + ") & readFile (" + readCount + ")");
                anyHooked = writeCount > 0;
            }

            if (dpStateClass != null) {
                hookDisplayPortStateMethods(dpStateClass);
                anyHooked = true;
            }
        } catch (Throwable t) {
            Config.logError("DpSysfsHook install failed", t);
        }
        return anyHooked;
    }

    private static void hookDisplayPortStateMethods(Class<?> dpStateClass) {
        try {
            for (Method m : dpStateClass.getDeclaredMethods()) {
                String name = m.getName();
                if (name.contains("DexMode") || name.contains("DisplayPort") || name.contains("DualMode")) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Config.logOnce("dpstate_" + name,
                                    "DisplayPortStateManager." + name + " invoked");
                        }
                    });
                }
            }
        } catch (Throwable t) {
            Config.logError("DisplayPortStateManager hook warning", t);
        }
    }

    private static void handleWriteFileBefore(XC_MethodHook.MethodHookParam param) {
        try {
            if (param.args == null || param.args.length < 2) {
                return;
            }
            int pathIdx = -1;
            for (int i = 0; i < param.args.length; i++) {
                if (param.args[i] instanceof String && ((String) param.args[i]).contains(DP_DEX_SUFFIX)) {
                    pathIdx = i;
                    break;
                }
            }
            if (pathIdx < 0 || !Config.isBlockDpDex()) {
                return;
            }

            int valIdx = pathIdx + 1;
            if (valIdx >= param.args.length || param.args[valIdx] == null) {
                return;
            }

            Object rawVal = param.args[valIdx];
            if (rawVal instanceof Number) {
                int oldVal = ((Number) rawVal).intValue();
                if ((oldVal & 0xF0) != 0) {
                    int newVal = oldVal & 0x0F;
                    param.args[valIdx] = Integer.valueOf(newVal);
                    sSimulatedDexOn = true;
                    Config.log("dp_sec/dex write intercepted (int): " + oldVal
                            + " -> " + newVal + " (keeping kernel in MIRROR/120Hz)");
                } else if (oldVal == 0) {
                    sSimulatedDexOn = false;
                }
                return;
            }

            if (rawVal instanceof String) {
                String strVal = ((String) rawVal).trim();
                // Never interfere with SkipAdapterCheck or HMD commands.
                if (strVal.startsWith("SkipAdapterCheck") || strVal.startsWith("HMD")) {
                    return;
                }
                Integer parsed = parseIntSafe(strVal);
                if (parsed == null) {
                    return;
                }
                int oldVal = parsed;
                if ((oldVal & 0xF0) != 0) {
                    int newVal = oldVal & 0x0F;
                    param.args[valIdx] = Integer.toString(newVal);
                    sSimulatedDexOn = true;
                    Config.log("dp_sec/dex write intercepted (str): " + strVal
                            + " -> " + newVal + " (keeping kernel in MIRROR/120Hz)");
                } else if (oldVal == 0) {
                    sSimulatedDexOn = false;
                }
            }
        } catch (Throwable t) {
            Config.logError("Error in DpSysfsHook.writeFile", t);
        }
    }

    private static void handleReadFileAfter(XC_MethodHook.MethodHookParam param) {
        try {
            if (param.args == null || param.args.length < 1 || !Config.isBlockDpDex()) {
                return;
            }
            boolean isDpDexPath = false;
            for (Object arg : param.args) {
                if (arg instanceof String && ((String) arg).contains(DP_DEX_SUFFIX)) {
                    isDpDexPath = true;
                    break;
                }
            }
            if (!isDpDexPath) {
                return;
            }

            Object result = param.getResult();
            boolean extActive = DisplayModeHook.hasActiveExternalDisplay();
            Config.logOnce("dp_read_" + result + "_" + sSimulatedDexOn + "_" + extActive,
                    "dp_sec/dex read observed: real=" + result
                            + ", simulatedDexOn=" + sSimulatedDexOn
                            + ", extActive=" + extActive);

            if (!sSimulatedDexOn || !extActive || result == null) {
                return;
            }

            if (result instanceof Integer && ((Integer) result) == 0) {
                param.setResult(Integer.valueOf(1));
                Config.logOnce("dp_spoof_int", "dp_sec/dex read spoofed: 0 -> 1");
            } else if (result instanceof String && "0".equals(((String) result).trim())) {
                param.setResult("1");
                Config.logOnce("dp_spoof_str", "dp_sec/dex read spoofed: \"0\" -> \"1\"");
            }
        } catch (Throwable t) {
            Config.logError("Error in DpSysfsHook.readFile", t);
        }
    }

    private static Integer parseIntSafe(String s) {
        try {
            return Integer.valueOf(Integer.parseInt(s));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
