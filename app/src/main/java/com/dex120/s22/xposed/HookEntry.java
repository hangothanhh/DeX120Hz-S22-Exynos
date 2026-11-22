// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 hangothanhh

package com.dex120.s22.xposed;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class HookEntry implements IXposedHookLoadPackage {
    private static final String SELF_PACKAGE = "com.dex120.s22";
    private static final String SDHMS_PACKAGE = "com.sec.android.sdhms";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || lpparam.packageName == null) {
            return;
        }

        if (SELF_PACKAGE.equals(lpparam.packageName)) {
            hookSelfStatus(lpparam.classLoader);
            return;
        }

        if (SDHMS_PACKAGE.equals(lpparam.packageName)) {
            SdhmsDfsHook.installInSdhms(lpparam.classLoader);
            return;
        }

        if ("android".equals(lpparam.packageName)
                && (lpparam.processName == null || "android".equals(lpparam.processName))) {
            Config.log("Initializing DeX120Hz hooks in system_server...");
            int installed = 0;
            if (DpSysfsHook.install(lpparam.classLoader)) installed++;
            if (LowRefreshTokenHook.install(lpparam.classLoader)) installed++;
            if (DisplayModeHook.install(lpparam.classLoader)) installed++;
            if (VirtualDisplaySyncHook.install(lpparam.classLoader)) installed++;
            Config.log("Hook initialization complete: " + installed + "/4 modules installed.");
        }
    }

    private static void hookSelfStatus(ClassLoader cl) {
        try {
            Class<?> statusClass = XposedHelpers.findClassIfExists(
                    "com.dex120.s22.ModuleStatus", cl);
            if (statusClass == null) {
                return;
            }
            XposedHelpers.findAndHookMethod(statusClass, "isActive", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(Boolean.TRUE);
                }
            });
        } catch (Throwable t) {
            Config.logError("Failed to hook ModuleStatus", t);
        }
    }
}
