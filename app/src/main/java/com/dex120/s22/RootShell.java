package com.dex120.s22;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class RootShell {
    private static final Pattern PAT_GITHUB_TOKEN =
            Pattern.compile("\\b(?:gh[pousr]_[A-Za-z0-9_]{16,}|github_pat_[A-Za-z0-9_]{20,})\\b");
    private static final Pattern PAT_KV_SECRET = Pattern.compile(
            "(?i)\\b(token|password|passwd|secret|api[_-]?key|authorization|bearer)\\b"
            + "(\\s*[:=]\\s*)(?!\\[DMS\\]|SiopLimiter)(?:Bearer\\s+)?\\S+");
    private static final Pattern PAT_SERIAL_KV = Pattern.compile(
            "(?i)(\\[?(?:ro\\.(?:boot\\.)?)?serial(?:no|number)?\\]?\\s*[:=]\\s*)\\[?[A-Za-z0-9_-]{6,}\\]?");
    private static final Pattern PAT_SAMSUNG_SERIAL =
            Pattern.compile("\\b(?=R[A-Z0-9]*\\d)R[A-Z0-9]{10}\\b");
    private static final Pattern PAT_IMEI = Pattern.compile(
            "(?i)(\\[?imei\\d?\\]?\\s*[:=]\\s*)\\[?\\d{14,17}\\]?");
    private static final Pattern PAT_MAC = Pattern.compile(
            "\\b(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}\\b");
    private static final Pattern PAT_PRIVATE_IP = Pattern.compile(
            "\\b(?:(?:10|127)\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"
            + "|192\\.168\\.\\d{1,3}\\.\\d{1,3}"
            + "|172\\.(?:1[6-9]|2\\d|3[0-1])\\.\\d{1,3}\\.\\d{1,3})\\b");
    private static final Pattern PAT_EMAIL = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9-]*[A-Za-z][A-Za-z0-9-]*\\.)+[A-Za-z]{2,}\\b");
    public static final class Result {
        public final int exitCode;
        public final String stdout;
        public final String stderr;

        public Result(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public boolean isOk() {
            return exitCode == 0;
        }
    }

    private RootShell() {}

    public static Result exec(String command) {
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command).start();
            StringBuilder out = new StringBuilder();
            StringBuilder err = new StringBuilder();

            Thread outReader = readAsync(process.getInputStream(), out);
            Thread errReader = readAsync(process.getErrorStream(), err);

            boolean finished = process.waitFor(6, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new Result(-1, out.toString(), "Command timed out");
            }
            outReader.join(1000);
            errReader.join(1000);
            return new Result(process.exitValue(), out.toString().trim(), err.toString().trim());
        } catch (Throwable t) {
            return new Result(-1, "", t.getClass().getSimpleName() + ": " + t.getMessage());
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Throwable ignored) {}
            }
        }
    }

    private static Thread readAsync(InputStream stream, StringBuilder target) {
        Thread thread = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (target.length() > 0) target.append('\n');
                    target.append(line);
                }
            } catch (Throwable ignored) {}
        });
        thread.start();
        return thread;
    }

    public static boolean hasRoot() {
        Result res = exec("id");
        return res.isOk() && res.stdout.contains("uid=0");
    }

    public static Result putGlobalInt(String key, int value) {
        return exec("settings put global " + key + " " + value);
    }

    public static Result applySamHelper120Hz() {
        String cmd = "settings put secure refresh_rate_mode 1; "
                + "settings put system refresh_rate_mode 1; "
                + "settings put system min_refresh_rate 120.0; "
                + "settings put system peak_refresh_rate 120.0; "
                + "settings put global low_power 0; "
                + "service call SurfaceFlinger 1122 i32 60 >/dev/null 2>&1";
        return exec(cmd);
    }

    public static Result resetRefreshRate() {
        String cmd = "settings delete system min_refresh_rate; "
                + "settings put system peak_refresh_rate 120.0; "
                + "settings put secure refresh_rate_mode 1; "
                + "settings put system refresh_rate_mode 1";
        return exec(cmd);
    }

    public static String readDpDexSysfs() {
        Result res = exec("if [ -r /sys/class/dp_sec/dex ]; then cat /sys/class/dp_sec/dex; "
                + "else echo 'N/A (missing)'; fi");
        if (!res.isOk()) {
            return "Lỗi đọc (cần root): " + res.stderr;
        }
        return res.stdout.isEmpty() ? "(empty)" : res.stdout;
    }

    public static String collectDiagnostics() {
        StringBuilder sb = new StringBuilder();

        sb.append("=== 0. Device & Module Summary ===\n");
        sb.append("module_version=1.0-s22-a12\n");
        sb.append("lsposed_active=").append(ModuleStatus.isActive()).append("\n");
        Result summary = exec(
                "echo android=$(getprop ro.build.version.release) api=$(getprop ro.build.version.sdk); "
                + "echo kernel=$(uname -r); "
                + "echo hardware=$(getprop ro.hardware) platform=$(getprop ro.board.platform); "
                + "echo vendor_model=$(getprop ro.product.vendor.model) device=$(getprop ro.product.vendor.device); "
                + "echo vsync_mode=$(getprop vendor.hwc.exynos.vsync_mode); "
                + "id");
        sb.append(summary.isOk() ? summary.stdout : summary.stderr).append("\n\n");

        sb.append("=== 1. /sys/class/dp_sec/ ===\n");
        Result sysfs = exec("ls -la /sys/class/dp_sec/ 2>&1; echo 'dex='$(cat /sys/class/dp_sec/dex 2>&1)");
        sb.append(sysfs.isOk() ? sysfs.stdout : sysfs.stderr).append("\n\n");

        sb.append("=== 2. Settings (refresh & dex120) ===\n");
        Result settings = exec(
                "echo min_refresh_rate=$(settings get system min_refresh_rate); "
                + "echo peak_refresh_rate=$(settings get system peak_refresh_rate); "
                + "echo system_refresh_rate_mode=$(settings get system refresh_rate_mode); "
                + "echo secure_refresh_rate_mode=$(settings get secure refresh_rate_mode); "
                + "echo low_power=$(settings get global low_power); "
                + "settings list global | grep '^dex120_'; "
                + "logcat -d -s SurfaceFlinger | grep 'DFS =' | tail -n 10");
        sb.append(settings.isOk() ? settings.stdout : settings.stderr).append("\n\n");

        sb.append("=== 3. Logcat DeX120Hz (last 80 lines) ===\n");
        Result logcat = exec("logcat -d -s DeX120Hz:I | tail -n 80");
        sb.append(logcat.stdout.isEmpty() ? "(Chưa có log — kiểm tra đã bật module trong LSPosed + scope android chưa)" : logcat.stdout);
        sb.append("\n\n");

        sb.append("=== 4. dumpsys display (EXTERNAL / Desktop / Votes) ===\n");
        Result display = exec("dumpsys display | grep -n -E 'type EXTERNAL|\"Desktop\"|mVotesByDisplay|DisplayInfo\\{' | head -n 40");
        sb.append(display.isOk() ? display.stdout : display.stderr).append("\n\n");

        sb.append("=== 5. dmesg DisplayPort / DeX ===\n");
        Result dmesg = exec("dmesg | grep -iE 'exynos-dp|dp_sec|best mode|filter out|cur_video|dex state|reconnect to' | tail -n 35");
        sb.append(dmesg.stdout.isEmpty() ? "(Không có log DP gần đây — hãy cắm cáp màn ngoài)" : dmesg.stdout);

        return sanitizeDiagnostics(sb.toString());
    }

    /** Redacts personal identifiers and secrets before sharing diagnostic reports. */
    public static String sanitizeDiagnostics(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String sanitized = raw;
        sanitized = PAT_GITHUB_TOKEN.matcher(sanitized).replaceAll("[REDACTED_TOKEN]");
        sanitized = PAT_KV_SECRET.matcher(sanitized).replaceAll("$1$2[REDACTED_SECRET]");
        sanitized = PAT_SERIAL_KV.matcher(sanitized).replaceAll("$1[REDACTED_SERIAL]");
        sanitized = PAT_SAMSUNG_SERIAL.matcher(sanitized).replaceAll("[REDACTED_SERIAL]");
        sanitized = PAT_IMEI.matcher(sanitized).replaceAll("$1[REDACTED_IMEI]");
        sanitized = PAT_MAC.matcher(sanitized).replaceAll("[REDACTED_MAC]");
        sanitized = PAT_PRIVATE_IP.matcher(sanitized).replaceAll("[REDACTED_IP]");
        sanitized = PAT_EMAIL.matcher(sanitized).replaceAll("[REDACTED_EMAIL]");
        return sanitized;
    }
}
