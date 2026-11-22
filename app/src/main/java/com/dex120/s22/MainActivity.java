package com.dex120.s22;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements DisplayManager.DisplayListener {
    private static final int BG_DARK = 0xFF0F1117;
    private static final int CARD_BG = 0xFF1A1E29;
    private static final int TEXT_PRIMARY = 0xFFF3F4F6;
    private static final int TEXT_MUTED = 0xFF9CA3AF;
    private static final int ACCENT_BLUE = 0xFF2563EB;
    private static final int ACCENT_GREEN = 0xFF059669;
    private static final int ACCENT_RED = 0xFFDC2626;
    private static final int ACCENT_SLATE = 0xFF334155;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private DisplayManager mDisplayManager;

    private TextView mModuleBadge;
    private TextView mRootBadge;
    private TextView mDisplayInfoText;
    private TextView mDiagOutputText;
    private Button mDiagButton;
    private String mLastDiagText = "";
    private volatile boolean mHasRoot = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDisplayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        setContentView(buildUi());
        refreshAllStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDisplayManager != null) {
            mDisplayManager.registerDisplayListener(this, mMainHandler);
        }
        refreshDisplayInfo();
    }

    @Override
    protected void onPause() {
        if (mDisplayManager != null) {
            mDisplayManager.unregisterDisplayListener(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onDisplayAdded(int displayId) {
        refreshDisplayInfo();
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        refreshDisplayInfo();
    }

    @Override
    public void onDisplayChanged(int displayId) {
        refreshDisplayInfo();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG_DARK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, dp(42), pad, dp(32));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("DeX 120Hz • S22 Exynos (A12)");
        title.setTextColor(TEXT_PRIMARY);
        title.setTextSize(21f);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("ReZygisk + LSPosed Hooks cho One UI 4.1 / s5e9925");
        subtitle.setTextColor(TEXT_MUTED);
        subtitle.setTextSize(13f);
        subtitle.setPadding(0, dp(2), 0, dp(12));
        root.addView(subtitle);

        // Status badges row
        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        mModuleBadge = makeBadge(ModuleStatus.isActive()
                ? "LSPosed: ĐÃ KÍCH HOẠT" : "LSPosed: CHƯA BẬT",
                ModuleStatus.isActive() ? ACCENT_GREEN : ACCENT_RED);
        mRootBadge = makeBadge("Root: Đang kiểm tra...", ACCENT_SLATE);

        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        b1.setMarginEnd(dp(8));
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        badgeRow.addView(mModuleBadge, b1);
        badgeRow.addView(mRootBadge, b2);
        root.addView(badgeRow);

        // Card 1: AMOLED Black Screen
        LinearLayout blackCard = makeCard();
        blackCard.addView(makeSectionTitle("1. Tắt màn giả AMOLED (giữ VSync 120Hz)"));
        blackCard.addView(makeMutedNote(
                "Android 12 SurfaceFlinger luôn tụt về 60Hz (SyntheticVSync 16ms) nếu tắt màn hình thật bằng nút nguồn. "
                + "Mở màn đen #000000 dưới đây để tắt pixel OLED mà vẫn giữ nhịp VSync 120Hz cho DeX."));
        Button blackBtn = makeButton("Mở Màn đen AMOLED (Giữ 120Hz)", ACCENT_BLUE);
        blackBtn.setOnClickListener(v ->
                startActivity(new Intent(this, BlackScreenActivity.class)));
        blackCard.addView(blackBtn);
        root.addView(blackCard);

        // Card 2: Display & Sysfs live info
        LinearLayout infoCard = makeCard();
        infoCard.addView(makeSectionTitle("2. Trạng thái màn hình & Kernel DP"));
        mDisplayInfoText = new TextView(this);
        mDisplayInfoText.setTextColor(TEXT_PRIMARY);
        mDisplayInfoText.setTextSize(12f);
        mDisplayInfoText.setTypeface(Typeface.MONOSPACE);
        mDisplayInfoText.setPadding(0, dp(4), 0, dp(10));
        infoCard.addView(mDisplayInfoText);

        Button refreshBtn = makeButton("Làm mới thông số Display / dp_sec", ACCENT_SLATE);
        refreshBtn.setOnClickListener(v -> refreshAllStatus());
        infoCard.addView(refreshBtn);
        root.addView(infoCard);

        // Card 3: Quick Refresh Controls
        LinearLayout hzCard = makeCard();
        hzCard.addView(makeSectionTitle("3. Ép 120Hz trên máy (giống SamHelper)"));
        LinearLayout hzBtnRow = new LinearLayout(this);
        hzBtnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button lock120Btn = makeButton("Ép cứng 120Hz", ACCENT_GREEN);
        lock120Btn.setOnClickListener(v -> runRootAction(
                "Đang ép min/peak = 120Hz...",
                RootShell::applySamHelper120Hz,
                "Đã ép cứng 120Hz & tắt Tiết kiệm pin"));

        Button resetHzBtn = makeButton("Khôi phục Adaptive", ACCENT_SLATE);
        resetHzBtn.setOnClickListener(v -> runRootAction(
                "Đang khôi phục mặc định...",
                RootShell::resetRefreshRate,
                "Đã khôi phục cấu hình Hz mặc định"));

        LinearLayout.LayoutParams h1 = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        h1.setMarginEnd(dp(8));
        LinearLayout.LayoutParams h2 = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        hzBtnRow.addView(lock120Btn, h1);
        hzBtnRow.addView(resetHzBtn, h2);
        hzCard.addView(hzBtnRow);
        root.addView(hzCard);

        // Card 4: Hook switches
        LinearLayout hookCard = makeCard();
        hookCard.addView(makeSectionTitle("4. Công tắc Hook trong system_server"));
        hookCard.addView(makeMutedNote(
                "Bật module trong LSPosed (tick 3 mục: 'Hệ thống Android' / 'android', 'com.sec.android.sdhms', và 'DeX 120Hz S22'), "
                + "sau đó Soft Reboot để 5 lớp hook nạp vào system_server & SDHMS. Các công tắc dưới áp dụng ngay lập tức:"));

        addSettingToggle(hookCard, "Bật toàn bộ Hook DeX 120Hz",
                "Công tắc tổng cho cả 5 lớp hook (DP, Token, DisplayMode, VirtualDisplay, SDHMS DFS)", ModuleStatus.KEY_ENABLED, 1);
        addSettingToggle(hookCard, "Hook 1: Giữ Kernel DP ở Mirror (120Hz)",
                "Chặn ghi DEX_ON (0x10/0x11) vào /sys/class/dp_sec/dex", ModuleStatus.KEY_BLOCK_DP_DEX, 1);
        addSettingToggle(hookCard, "Hook 2: Chặn khóa 60Hz toàn cục",
                "Bỏ qua acquireLowRefreshRateToken của DeX HardwareManager", ModuleStatus.KEY_BLOCK_TOKEN, 1);
        addSettingToggle(hookCard, "Hook 3: Mở khóa DisplayModeDirector",
                "Nâng baseModeId & RefreshRateRange màn ngoài lên 120Hz", ModuleStatus.KEY_UNLOCK_MODES, 1);
        addSettingToggle(hookCard, "Hook 4: Đồng bộ Hz vào DeX VirtualDisplay",
                "Cập nhật VirtualDisplay 'Desktop' khớp Hz thật màn ngoài", ModuleStatus.KEY_SYNC_VIRTUAL, 1);
        addSettingToggle(hookCard, "Test Mode (màn 60Hz): Ép Desktop 120Hz",
                "Chỉ dùng để test VirtualDisplay báo 120Hz khi chưa có màn 120Hz (tắt khi dùng thật)",
                ModuleStatus.KEY_FORCE_VIRTUAL_120, 0);
        root.addView(hookCard);

        // Card 5: Diagnostics
        LinearLayout diagCard = makeCard();
        diagCard.addView(makeSectionTitle("5. Chẩn đoán & Thu thập Log"));
        LinearLayout diagBtnRow = new LinearLayout(this);
        diagBtnRow.setOrientation(LinearLayout.HORIZONTAL);

        mDiagButton = makeButton("Chạy chẩn đoán", ACCENT_BLUE);
        mDiagButton.setOnClickListener(v -> runDiagnostics());

        Button copyBtn = makeButton("Copy Log", ACCENT_SLATE);
        copyBtn.setOnClickListener(v -> {
            if (mLastDiagText.isEmpty()) {
                toast("Hãy bấm 'Chạy chẩn đoán' trước");
                return;
            }
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("DeX120Hz Diagnostics", mLastDiagText));
                toast("Đã copy kết quả chẩn đoán");
            }
        });

        LinearLayout.LayoutParams d1 = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f);
        d1.setMarginEnd(dp(8));
        LinearLayout.LayoutParams d2 = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        diagBtnRow.addView(mDiagButton, d1);
        diagBtnRow.addView(copyBtn, d2);
        diagCard.addView(diagBtnRow);

        mDiagOutputText = new TextView(this);
        mDiagOutputText.setTextColor(0xFFD1D5DB);
        mDiagOutputText.setTextSize(11f);
        mDiagOutputText.setTypeface(Typeface.MONOSPACE);
        mDiagOutputText.setPadding(dp(10), dp(10), dp(10), dp(10));
        mDiagOutputText.setText(
                "Nhấn 'Chạy chẩn đoán' sau khi bật DeX hoặc cắm cáp để kiểm tra logcat, dumpsys và dmesg.\n"
                + "(Hệ thống tự động ẩn Serial, IMEI, MAC, IP nội bộ và Token trước khi Copy Log.)");
        GradientDrawable termBg = new GradientDrawable();
        termBg.setColor(0xFF0A0C10);
        termBg.setCornerRadius(dp(8));
        mDiagOutputText.setBackground(termBg);
        LinearLayout.LayoutParams diagOutLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        diagOutLp.topMargin = dp(10);
        diagCard.addView(mDiagOutputText, diagOutLp);

        root.addView(diagCard);
        return scroll;
    }

    private void refreshAllStatus() {
        boolean active = ModuleStatus.isActive();
        mModuleBadge.setText(active ? "LSPosed: ĐÃ KÍCH HOẠT" : "LSPosed: CHƯA BẬT");
        setRoundedBackground(mModuleBadge, active ? ACCENT_GREEN : ACCENT_RED, 8);

        refreshDisplayInfo();

        mExecutor.execute(() -> {
            boolean rootOk = RootShell.hasRoot();
            String dpDex = rootOk ? RootShell.readDpDexSysfs() : "(cần root)";
            mMainHandler.post(() -> {
                mHasRoot = rootOk;
                mRootBadge.setText(rootOk ? "Root: Sẵn sàng" : "Root: Chưa cấp quyền");
                setRoundedBackground(mRootBadge, rootOk ? ACCENT_GREEN : ACCENT_RED, 8);
                refreshDisplayInfoWithSysfs(dpDex);
            });
        });
    }

    private void refreshDisplayInfo() {
        refreshDisplayInfoWithSysfs(mHasRoot ? "..." : "(chờ kiểm tra root)");
        if (mHasRoot) {
            mExecutor.execute(() -> {
                String dpDex = RootShell.readDpDexSysfs();
                mMainHandler.post(() -> refreshDisplayInfoWithSysfs(dpDex));
            });
        }
    }

    private void refreshDisplayInfoWithSysfs(String dpDexValue) {
        if (mDisplayManager == null || mDisplayInfoText == null) return;
        StringBuilder sb = new StringBuilder();
        Display[] displays = mDisplayManager.getDisplays();
        for (Display d : displays) {
            int type = getDisplayTypeSafe(d);
            Display.Mode cur = d.getMode();
            Display.Mode[] supported = d.getSupportedModes();
            sb.append(String.format(Locale.US, "• ID %d (%s) \"%s\"\n",
                    d.getDisplayId(), displayTypeLabel(type), d.getName()));
            if (cur != null) {
                sb.append(String.format(Locale.US, "  Active: %dx%d @ %.2f Hz (modeId=%d)\n",
                        cur.getPhysicalWidth(), cur.getPhysicalHeight(),
                        cur.getRefreshRate(), cur.getModeId()));
            }
            if (type != 1 && supported != null && supported.length > 0) {
                sb.append("  Supported: ");
                int maxShow = Math.min(supported.length, 6);
                for (int i = 0; i < maxShow; i++) {
                    if (i > 0) sb.append(", ");
                    Display.Mode m = supported[i];
                    sb.append(String.format(Locale.US, "%dx%d@%.0f",
                            m.getPhysicalWidth(), m.getPhysicalHeight(), m.getRefreshRate()));
                }
                if (supported.length > maxShow) {
                    sb.append(" ...(+").append(supported.length - maxShow).append(")");
                }
                sb.append("\n");
            }
        }
        sb.append("• /sys/class/dp_sec/dex = ").append(dpDexValue);
        try {
            int mode = Settings.Secure.getInt(getContentResolver(), "refresh_rate_mode", -1);
            float minHz = Settings.System.getFloat(getContentResolver(), "min_refresh_rate", -1f);
            float peakHz = Settings.System.getFloat(getContentResolver(), "peak_refresh_rate", -1f);
            String modeLabel = (mode == 0) ? "0 (Chuẩn 60Hz)"
                    : (mode == 1) ? "1 (Tương thích)"
                    : (mode == 2) ? "2 (Cao)"
                    : String.valueOf(mode);
            sb.append("\n• refresh_rate_mode (Secure) = ").append(modeLabel);
            sb.append(String.format(Locale.US, "\n• min/peak_refresh_rate = %.1f / %.1f Hz", minHz, peakHz));
            if (minHz > 120.05f || peakHz > 120.05f) {
                sb.append("\n  [!] Cảnh báo: 120.1 Hz (SamHelper) sẽ bị từ chối — hãy bấm 'Ép cứng 120Hz' để đặt 120.0!");
            }
        } catch (Throwable ignored) {}
        mDisplayInfoText.setText(sb.toString().trim());
    }

    private static int getDisplayTypeSafe(Display d) {
        try {
            Object res = Display.class.getMethod("getType").invoke(d);
            if (res instanceof Integer) {
                return (Integer) res;
            }
        } catch (Throwable ignored) {}
        return d.getDisplayId() == Display.DEFAULT_DISPLAY ? 1 : -1;
    }

    private static String displayTypeLabel(int type) {
        switch (type) {
            case 1: return "INTERNAL";
            case 2: return "EXTERNAL";
            case 3: return "WIFI";
            case 4: return "OVERLAY";
            case 5: return "VIRTUAL";
            default: return "TYPE_" + type;
        }
    }

    private void addSettingToggle(
            LinearLayout parent, String title, String desc, String key, int defaultVal) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(TEXT_PRIMARY);
        tvTitle.setTextSize(14f);
        texts.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(desc);
        tvDesc.setTextColor(TEXT_MUTED);
        tvDesc.setTextSize(11.5f);
        texts.addView(tvDesc);

        Switch sw = new Switch(this);
        boolean current = Settings.Global.getInt(getContentResolver(), key, defaultVal) != 0;
        sw.setChecked(current);
        bindToggleListener(sw, key);

        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tl.setMarginEnd(dp(10));
        row.addView(texts, tl);
        row.addView(sw);
        parent.addView(row);
    }

    private void bindToggleListener(android.widget.CompoundButton sw, String key) {
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int val = isChecked ? 1 : 0;
            mExecutor.execute(() -> {
                RootShell.Result res = RootShell.putGlobalInt(key, val);
                if (!res.isOk()) {
                    mMainHandler.post(() -> {
                        toast("Cần quyền Root để ghi " + key);
                        buttonView.setOnCheckedChangeListener(null);
                        buttonView.setChecked(!isChecked);
                        bindToggleListener(buttonView, key);
                    });
                }
            });
        });
    }

    private interface RootTask {
        RootShell.Result run();
    }

    private void runRootAction(String startMsg, RootTask task, String okMsg) {
        toast(startMsg);
        mExecutor.execute(() -> {
            RootShell.Result res = task.run();
            mMainHandler.post(() -> {
                if (res.isOk()) {
                    toast(okMsg);
                    refreshDisplayInfo();
                } else {
                    toast("Lỗi Root: " + (res.stderr.isEmpty() ? res.stdout : res.stderr));
                }
            });
        });
    }

    private void runDiagnostics() {
        mDiagButton.setEnabled(false);
        mDiagOutputText.setText("Đang thu thập logcat / dumpsys / dmesg...");
        mExecutor.execute(() -> {
            String output = RootShell.collectDiagnostics();
            mMainHandler.post(() -> {
                mLastDiagText = output;
                mDiagOutputText.setText(output);
                mDiagButton.setEnabled(true);
            });
        });
    }

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(14);
        card.setPadding(pad, pad, pad, pad);
        setRoundedBackground(card, CARD_BG, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView makeSectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(TEXT_PRIMARY);
        tv.setTextSize(15.5f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 0, 0, dp(6));
        return tv;
    }

    private TextView makeMutedNote(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(TEXT_MUTED);
        tv.setTextSize(12.5f);
        tv.setPadding(0, 0, 0, dp(10));
        return tv;
    }

    private TextView makeBadge(String text, int bgColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(12f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(10), dp(8), dp(10), dp(8));
        setRoundedBackground(tv, bgColor, 8);
        return tv;
    }

    private Button makeButton(String text, int bgColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setAllCaps(false);
        btn.setTextSize(13.5f);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setPadding(dp(12), dp(10), dp(12), dp(10));
        setRoundedBackground(btn, bgColor, 8);
        return btn;
    }

    private void setRoundedBackground(View view, int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        view.setBackground(d);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
