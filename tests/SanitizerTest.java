// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 hangothanhh

import com.dex120.s22.RootShell;

public final class SanitizerTest {
    public static void main(String[] args) {
        String fakeToken = "ghp_" + "abcdefghijklmnopqrstuvwxyz123456";
        String fakeOauthToken = "gho_" + "1234567890abcdefghijklmnop";
        String fakeFinePat = "github_pat_" + "11AAAAAA0000bbbbccccdddd";
        String sample = String.join("\n",
                "=== 0. Device & Module Summary ===",
                "module_version=1.0.1-s22-a12",
                "[ro.serialno]: [R5AB12XYZ99]",
                "ro.boot.serialno=R5AB12XYZ99",
                "raw_serial=R5XY9876543",
                "imei1=123456789012345",
                "wlan0 mac=AA:BB:CC:DD:EE:FF",
                "adb tcpip=10.0.12.34:5555",
                "lan=192.168.1.50 work=172.16.5.9 upper172=172.31.255.1 public=172.32.0.1 loopback=127.0.0.1",
                "owner=test.user@example.com",
                "Authorization: Bearer " + fakeToken,
                "cli_auth " + fakeOauthToken,
                "pat_note " + fakeFinePat,
                "password=supersecret_pw",
                "dex120_block_token=1",
                "Reset SurfaceFlinger DFS=60 (token:[DMS]HardwareManager)",
                "Reset SurfaceFlinger DFS=60 (token:SiopLimiterHrr)",
                "Upgraded external display 6 baseModeId: 9 (1920x1080@60.0Hz) -> 8 (@75.0Hz)",
                "DisplayDevice{4633128672291735999, internal, primary}",
                "VSync period: 13.33ms",
                "/sys/class/dp_sec/dex=0"
        );

        String out = RootShell.sanitizeDiagnostics(sample);

        assertNotContains(out, "R5AB12XYZ99", "Samsung serial kv");
        assertNotContains(out, "R5XY9876543", "Samsung standalone serial");
        assertNotContains(out, "123456789012345", "IMEI");
        assertNotContains(out, "AA:BB:CC:DD:EE:FF", "MAC address");
        assertNotContains(out, "10.0.12.34", "10.x private IP");
        assertNotContains(out, "192.168.1.50", "192.168.x private IP");
        assertNotContains(out, "172.16.5.9", "172.16.x private IP");
        assertNotContains(out, "172.31.255.1", "172.31.x private IP");
        assertNotContains(out, "127.0.0.1", "loopback IP");
        assertNotContains(out, "test.user@example.com", "email address");
        assertNotContains(out, fakeToken, "GitHub ghp token");
        assertNotContains(out, fakeOauthToken, "GitHub gho token");
        assertNotContains(out, fakeFinePat, "GitHub fine-grained PAT");
        assertNotContains(out, "supersecret_pw", "password value");

        assertContains(out, "[REDACTED_SERIAL]", "serial marker");
        assertContains(out, "[REDACTED_IMEI]", "IMEI marker");
        assertContains(out, "[REDACTED_MAC]", "MAC marker");
        assertContains(out, "[REDACTED_IP]", "IP marker");
        assertContains(out, "[REDACTED_EMAIL]", "email marker");
        assertContains(out, "[REDACTED_TOKEN]", "token marker");
        assertContains(out, "[REDACTED_SECRET]", "secret marker");
        assertContains(out, "172.32.0.1", "public IPv4 outside 172.16-31 preserved");

        assertContains(out, "dex120_block_token=1", "setting key dex120_block_token preserved");
        assertContains(out, "Reset SurfaceFlinger DFS=60 (token:[DMS]HardwareManager)", "DMS token tag preserved");
        assertContains(out, "Reset SurfaceFlinger DFS=60 (token:SiopLimiterHrr)", "SiopLimiter token tag preserved");
        assertContains(out, "1920x1080@60.0Hz", "@Hz display mode preserved");
        assertContains(out, "@75.0Hz", "@75.0Hz display mode preserved");
        assertContains(out, "4633128672291735999", "DisplayDevice ID preserved");
        assertContains(out, "/sys/class/dp_sec/dex=0", "sysfs node preserved");

        System.out.println("[OK] SanitizerTest: all redaction & preservation assertions passed");
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError("Expected '" + label + "' (" + needle + ") in:\n" + haystack);
        }
    }

    private static void assertNotContains(String haystack, String needle, String label) {
        if (haystack.contains(needle)) {
            throw new AssertionError("Sensitive '" + label + "' (" + needle + ") leaked in:\n" + haystack);
        }
    }
}
