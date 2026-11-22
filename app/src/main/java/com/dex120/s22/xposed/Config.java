package com.dex120.s22.xposed;

import android.content.ContentResolver;
import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XposedBridge;

public final class Config {
    public static final String LOG_TAG = "DeX120Hz";
    public static final String XPOSED_PREFIX = "[DeX120Hz] ";

    public static final String KEY_ENABLED = "dex120_enabled";
    public static final String KEY_BLOCK_DP_DEX = "dex120_block_dp_dex";
    public static final String KEY_BLOCK_TOKEN = "dex120_block_token";
    public static final String KEY_UNLOCK_MODES = "dex120_unlock_modes";
    public static final String KEY_SYNC_VIRTUAL = "dex120_sync_virtual";
    public static final String KEY_FORCE_VIRTUAL_120 = "dex120_force_virtual_120";
    public static final String KEY_TARGET_HZ = "dex120_target_hz";

    private static final long REFRESH_INTERVAL_MS = 2000L;

    private static volatile Context sSystemContext;
    private static volatile long sLastRefreshMs = 0L;

    private static volatile boolean sEnabled = true;
    private static volatile boolean sBlockDpDex = true;
    private static volatile boolean sBlockToken = true;
    private static volatile boolean sUnlockModes = true;
    private static volatile boolean sSyncVirtual = true;
    private static volatile boolean sForceVirtual120 = false;
    private static volatile int sTargetHz = 120;

    private static final Set<String> sLoggedTags = ConcurrentHashMap.newKeySet();

    private Config() {}

    public static void log(String msg) {
        try {
            Log.i(LOG_TAG, msg);
        } catch (Throwable ignored) {}
        try {
            XposedBridge.log(XPOSED_PREFIX + msg);
        } catch (Throwable ignored) {}
    }

    public static void logOnce(String key, String msg) {
        if (sLoggedTags.add(key)) {
            log(msg);
        }
    }

    public static void logError(String msg, Throwable t) {
        try {
            Log.e(LOG_TAG, msg, t);
        } catch (Throwable ignored) {}
        try {
            XposedBridge.log(XPOSED_PREFIX + msg + ": " + Log.getStackTraceString(t));
        } catch (Throwable ignored) {}
    }

    public static void setSystemContext(Context ctx) {
        if (ctx != null && sSystemContext == null) {
            sSystemContext = ctx;
            refreshNow();
        }
    }

    public static Context getSystemContext() {
        Context ctx = sSystemContext;
        if (ctx != null) {
            return ctx;
        }
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Method m = at.getMethod("currentApplication");
            Object app = m.invoke(null);
            if (app instanceof Context) {
                sSystemContext = (Context) app;
                return sSystemContext;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static void refreshIfNeeded() {
        long now = SystemClock.uptimeMillis();
        if (now - sLastRefreshMs >= REFRESH_INTERVAL_MS) {
            refreshNow();
        }
    }

    public static synchronized void refreshNow() {
        sLastRefreshMs = SystemClock.uptimeMillis();
        Context ctx = getSystemContext();
        if (ctx == null) {
            return;
        }
        try {
            ContentResolver cr = ctx.getContentResolver();
            if (cr == null) {
                return;
            }
            sEnabled = Settings.Global.getInt(cr, KEY_ENABLED, 1) != 0;
            sBlockDpDex = Settings.Global.getInt(cr, KEY_BLOCK_DP_DEX, 1) != 0;
            sBlockToken = Settings.Global.getInt(cr, KEY_BLOCK_TOKEN, 1) != 0;
            sUnlockModes = Settings.Global.getInt(cr, KEY_UNLOCK_MODES, 1) != 0;
            sSyncVirtual = Settings.Global.getInt(cr, KEY_SYNC_VIRTUAL, 1) != 0;
            sForceVirtual120 = Settings.Global.getInt(cr, KEY_FORCE_VIRTUAL_120, 0) != 0;
            int hz = Settings.Global.getInt(cr, KEY_TARGET_HZ, 120);
            if (hz < 60 || hz > 240) {
                hz = 120;
            }
            sTargetHz = hz;
        } catch (Throwable ignored) {
            // Early boot before SettingsProvider is ready: keep safe defaults.
        }
    }

    public static boolean isEnabled() {
        refreshIfNeeded();
        return sEnabled;
    }

    public static boolean isBlockDpDex() {
        refreshIfNeeded();
        return sEnabled && sBlockDpDex;
    }

    public static boolean isBlockToken() {
        refreshIfNeeded();
        return sEnabled && sBlockToken;
    }

    public static boolean isUnlockModes() {
        refreshIfNeeded();
        return sEnabled && sUnlockModes;
    }

    public static boolean isSyncVirtual() {
        refreshIfNeeded();
        return sEnabled && sSyncVirtual;
    }

    public static boolean isForceVirtual120() {
        refreshIfNeeded();
        return sEnabled && sSyncVirtual && sForceVirtual120;
    }

    public static int getTargetHz() {
        refreshIfNeeded();
        return sTargetHz;
    }

    // Shared reflection helpers to keep hook code concise and avoid optional XposedHelpers stubs.
    public static Field findField(Class<?> clazz, String name) {
        Class<?> cur = clazz;
        while (cur != null && cur != Object.class) {
            try {
                Field f = cur.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }

    public static Object getFieldSafe(Object target, String name) {
        if (target == null) return null;
        try {
            Field f = findField(target.getClass(), name);
            return f != null ? f.get(target) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object getStaticFieldSafe(Class<?> clazz, String name) {
        if (clazz == null) return null;
        try {
            Field f = findField(clazz, name);
            return f != null ? f.get(null) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static int getIntFieldSafe(Object target, String name, int fallback) {
        Object val = getFieldSafe(target, name);
        return (val instanceof Number) ? ((Number) val).intValue() : fallback;
    }

    public static float getFloatFieldSafe(Object target, String name, float fallback) {
        Object val = getFieldSafe(target, name);
        return (val instanceof Number) ? ((Number) val).floatValue() : fallback;
    }

    public static boolean setFieldSafe(Object target, String name, Object value) {
        if (target == null) return false;
        try {
            Field f = findField(target.getClass(), name);
            if (f != null) {
                f.set(target, value);
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void dumpMethodsOnce(Class<?> clazz, String label) {
        if (clazz == null) {
            logOnce("dump_" + label, label + ": class not found");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(" (").append(clazz.getName()).append(") methods: ");
        Method[] methods = clazz.getDeclaredMethods();
        int limit = Math.min(methods.length, 40);
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(", ");
            sb.append(methods[i].getName());
        }
        if (methods.length > limit) {
            sb.append(" ...(+").append(methods.length - limit).append(")");
        }
        logOnce("dump_" + label, sb.toString());
    }
}
