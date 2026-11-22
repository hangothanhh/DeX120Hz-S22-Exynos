// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 hangothanhh

package com.dex120.s22;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

public class BlackScreenActivity extends Activity {
    private static final long TRIPLE_TAP_WINDOW_MS = 700L;
    private static final long DOUBLE_BACK_WINDOW_MS = 1200L;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private TextView mHintView;
    private int mTapCount = 0;
    private long mFirstTapTime = 0L;
    private long mLastBackTime = 0L;

    private final Runnable mHideHintTask = () -> {
        if (mHintView != null) {
            mHintView.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        float targetHz = 120f;
        try {
            targetHz = Settings.Global.getInt(getContentResolver(), ModuleStatus.KEY_TARGET_HZ, 120);
        } catch (Throwable ignored) {}

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.setDecorFitsSystemWindows(false);

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = 0.001f;
        lp.preferredRefreshRate = targetHz;
        lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

        Display display = getDisplay();
        if (display != null) {
            Display.Mode bestMode = findHighestMode(display, targetHz);
            if (bestMode != null) {
                lp.preferredDisplayModeId = bestMode.getModeId();
            }
        }
        window.setAttributes(lp);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        mHintView = new TextView(this);
        mHintView.setTextColor(Color.argb(140, 255, 255, 255));
        mHintView.setTextSize(13f);
        mHintView.setGravity(Gravity.CENTER);
        mHintView.setPadding(48, 48, 48, 48);
        mHintView.setText("Màn đen AMOLED giữ VSync " + (int) targetHz + "Hz\n"
                + "(Màn điện thoại vẫn ON ở #000000 để SurfaceFlinger A12 không tụt 60Hz)\n\n"
                + "Chạm nhanh 3 lần liên tiếp hoặc bấm Back 2 lần để thoát");

        FrameLayout.LayoutParams tvLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        root.addView(mHintView, tvLp);

        root.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                long now = System.currentTimeMillis();
                if (mTapCount == 0 || now - mFirstTapTime > TRIPLE_TAP_WINDOW_MS) {
                    mTapCount = 1;
                    mFirstTapTime = now;
                } else {
                    mTapCount++;
                    if (mTapCount >= 3) {
                        finish();
                        return true;
                    }
                }
                showHintTemporarily(1800L);
            }
            return true;
        });

        setContentView(root);
        hideSystemBars();
        showHintTemporarily(3000L);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
    }

    @Override
    public void onBackPressed() {
        long now = System.currentTimeMillis();
        if (now - mLastBackTime <= DOUBLE_BACK_WINDOW_MS) {
            super.onBackPressed();
            return;
        }
        mLastBackTime = now;
        showHintTemporarily(1500L);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mHideHintTask);
        super.onDestroy();
    }

    private void showHintTemporarily(long durationMs) {
        if (mHintView == null) return;
        mHandler.removeCallbacks(mHideHintTask);
        mHintView.setVisibility(View.VISIBLE);
        mHandler.postDelayed(mHideHintTask, durationMs);
    }

    private void hideSystemBars() {
        try {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } catch (Throwable ignored) {}
    }

    private static Display.Mode findHighestMode(Display display, float targetHz) {
        Display.Mode current = display.getMode();
        if (current == null) return null;
        int w = current.getPhysicalWidth();
        int h = current.getPhysicalHeight();
        Display.Mode best = current;
        for (Display.Mode m : display.getSupportedModes()) {
            if (m.getPhysicalWidth() == w && m.getPhysicalHeight() == h) {
                if (m.getRefreshRate() <= targetHz + 1f
                        && m.getRefreshRate() > best.getRefreshRate()) {
                    best = m;
                }
            }
        }
        return best;
    }
}
