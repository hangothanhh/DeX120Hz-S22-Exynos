package com.dex120.s22.xposed;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcel;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class SdhmsDfsHook {
    private static final int SF_TX_SET_DFS = 1122;
    private static final String SF_DESCRIPTOR = "android.ui.ISurfaceComposer";
    private static final int DFS_DISABLE_VALUE = 60;
    private static final long[] RESET_DELAYS_MS = new long[] { 150L, 1000L, 3000L };

    private static volatile Handler sHandler;
    private static final Object HANDLER_LOCK = new Object();

    private SdhmsDfsHook() {}

    /** Hooks BinderProxy.transact in SDHMS to block dynamic FPS (DFS < 60) requests. */
    public static boolean installInSdhms(ClassLoader cl) {
        try {
            Class<?> binderProxyClass = XposedHelpers.findClassIfExists(
                    "android.os.BinderProxy", cl);
            if (binderProxyClass == null) {
                Config.log("SdhmsDfsHook: BinderProxy not found");
                return false;
            }

            int count = XposedBridge.hookAllMethods(
                    binderProxyClass, "transact", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args == null || param.args.length < 2
                                    || !(param.args[0] instanceof Integer)
                                    || ((Integer) param.args[0]) != SF_TX_SET_DFS
                                    || !(param.args[1] instanceof Parcel)) {
                                return;
                            }
                            if (!Config.isBlockToken()) {
                                return;
                            }
                            Parcel data = (Parcel) param.args[1];
                            int savedPos = data.dataPosition();
                            try {
                                data.setDataPosition(0);
                                data.enforceInterface(SF_DESCRIPTOR);
                                int dfs = data.readInt();
                                if (dfs > 0 && dfs < DFS_DISABLE_VALUE) {
                                    param.setResult(Boolean.TRUE);
                                    Config.log("Blocked SDHMS SurfaceFlinger DFS="
                                            + dfs + " (tx 1122)");
                                }
                            } catch (Throwable ignored) {
                                // Ignore unrelated binders using transaction code 1122.
                            } finally {
                                try {
                                    data.setDataPosition(savedPos);
                                } catch (Throwable ignored) {}
                            }
                        }
                    }).size();

            Config.log("SdhmsDfsHook: hooked BinderProxy.transact in com.sec.android.sdhms ("
                    + count + ")");
            return count > 0;
        } catch (Throwable t) {
            Config.logError("SdhmsDfsHook installInSdhms failed", t);
            return false;
        }
    }

    /** Sends SurfaceFlinger transaction 1122 with DFS=60 to restore full refresh rate. */
    public static void resetSurfaceFlingerDfs(String reason) {
        Parcel data = null;
        try {
            Class<?> smClass = Class.forName("android.os.ServiceManager");
            Method getService = smClass.getMethod("getService", String.class);
            Object sfObj = getService.invoke(null, "SurfaceFlinger");
            if (!(sfObj instanceof IBinder)) {
                return;
            }
            IBinder sf = (IBinder) sfObj;
            data = Parcel.obtain();
            data.writeInterfaceToken(SF_DESCRIPTOR);
            data.writeInt(DFS_DISABLE_VALUE);
            sf.transact(SF_TX_SET_DFS, data, null, 0);
            Config.log("Reset SurfaceFlinger DFS=60 (" + reason + ")");
        } catch (Throwable t) {
            Config.logError("Reset SurfaceFlinger DFS=60 failed (" + reason + ")", t);
        } finally {
            if (data != null) {
                try {
                    data.recycle();
                } catch (Throwable ignored) {}
            }
        }
    }

    /** Schedules staggered DFS=60 resets on a daemon handler thread to counter thermal drops. */
    public static void scheduleDfsReset(String reason) {
        Handler handler = getOrCreateHandler();
        if (handler == null) {
            return;
        }
        handler.removeCallbacksAndMessages(null);
        for (long delayMs : RESET_DELAYS_MS) {
            handler.postDelayed(() -> resetSurfaceFlingerDfs(reason), delayMs);
        }
    }

    private static Handler getOrCreateHandler() {
        Handler h = sHandler;
        if (h != null) {
            return h;
        }
        synchronized (HANDLER_LOCK) {
            if (sHandler == null) {
                HandlerThread thread = new HandlerThread("DeX120Hz-DFS");
                thread.setDaemon(true);
                thread.start();
                sHandler = new Handler(thread.getLooper());
            }
            return sHandler;
        }
    }
}
