// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 hangothanhh

import com.dex120.s22.ModuleStatus;
import com.dex120.s22.xposed.Config;
import com.dex120.s22.xposed.DpSysfsHook;

public final class HookLogicTest {
    private static class ParentFixture {
        protected int baseInt = 75;
    }

    private static final class ChildFixture extends ParentFixture {
        private float targetFloat = 120.0f;
        private String label = "Desktop";
    }

    public static void main(String[] args) {
        // 1. DpSysfsHook.maskDexWriteValue
        assertEquals(0, DpSysfsHook.maskDexWriteValue(0x10), "0x10 -> 0 (MIRROR)");
        assertEquals(1, DpSysfsHook.maskDexWriteValue(0x11), "0x11 -> 1 (MIRROR)");
        assertEquals(2, DpSysfsHook.maskDexWriteValue(0x12), "0x12 -> 2");
        assertEquals(0, DpSysfsHook.maskDexWriteValue(0x20), "0x20 -> 0");
        assertEquals(0, DpSysfsHook.maskDexWriteValue(0), "0 preserved");
        assertEquals(1, DpSysfsHook.maskDexWriteValue(1), "1 preserved");

        // 2. DpSysfsHook.isPassthroughDpCommand
        assertTrue(DpSysfsHook.isPassthroughDpCommand("SkipAdapterCheck"), "SkipAdapterCheck exact");
        assertTrue(DpSysfsHook.isPassthroughDpCommand("SkipAdapterCheck=1"), "SkipAdapterCheck param");
        assertTrue(DpSysfsHook.isPassthroughDpCommand("HMD"), "HMD exact");
        assertTrue(DpSysfsHook.isPassthroughDpCommand("HMD_ON"), "HMD suffix");
        assertFalse(DpSysfsHook.isPassthroughDpCommand("16"), "numeric 16 not passthrough");
        assertFalse(DpSysfsHook.isPassthroughDpCommand(""), "empty string not passthrough");
        assertFalse(DpSysfsHook.isPassthroughDpCommand(null), "null safe");

        // 3. DpSysfsHook.parseIntSafe
        assertEquals(Integer.valueOf(16), DpSysfsHook.parseIntSafe("16"), "parse 16");
        assertEquals(Integer.valueOf(0), DpSysfsHook.parseIntSafe("0"), "parse 0");
        assertTrue(DpSysfsHook.parseIntSafe("abc") == null, "parse non-numeric -> null");
        assertTrue(DpSysfsHook.parseIntSafe("") == null, "parse empty -> null");

        // 4. Config.sanitizeTargetHz
        assertEquals(60, Config.sanitizeTargetHz(60), "lower bound 60");
        assertEquals(120, Config.sanitizeTargetHz(120), "default 120");
        assertEquals(240, Config.sanitizeTargetHz(240), "upper bound 240");
        assertEquals(120, Config.sanitizeTargetHz(59), "below min -> 120");
        assertEquals(120, Config.sanitizeTargetHz(241), "above max -> 120");
        assertEquals(120, Config.sanitizeTargetHz(0), "zero -> 120");
        assertEquals(120, Config.sanitizeTargetHz(-10), "negative -> 120");

        // 5. Config reflection helpers (inherited + private fields + null safety)
        ChildFixture obj = new ChildFixture();
        assertTrue(Config.findField(ChildFixture.class, "baseInt") != null, "find inherited field");
        assertTrue(Config.findField(ChildFixture.class, "missingField") == null, "missing field -> null");
        assertEquals(75, Config.getIntFieldSafe(obj, "baseInt", -1), "getIntFieldSafe inherited");
        assertEquals(-1, Config.getIntFieldSafe(null, "baseInt", -1), "getIntFieldSafe null target");
        assertFloatEquals(120.0f, Config.getFloatFieldSafe(obj, "targetFloat", 60.0f), "getFloatFieldSafe");
        assertTrue(Config.setFieldSafe(obj, "baseInt", Integer.valueOf(120)), "setFieldSafe inherited");
        assertEquals(120, Config.getIntFieldSafe(obj, "baseInt", -1), "updated int value");
        assertEquals("Desktop", Config.getFieldSafe(obj, "label"), "getFieldSafe string");
        assertFalse(Config.setFieldSafe(null, "label", "X"), "setFieldSafe null target");

        // 6. ModuleStatus default
        assertFalse(ModuleStatus.isActive(), "ModuleStatus default false without Xposed");

        System.out.println("[OK] HookLogicTest: all pure-Java hook, DP mask, Hz clamp, and reflection checks passed");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Assertion failed [" + label + "]: expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertFloatEquals(float expected, float actual, String label) {
        if (Math.abs(expected - actual) > 0.0001f) {
            throw new AssertionError("Assertion failed [" + label + "]: expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean cond, String label) {
        if (!cond) {
            throw new AssertionError("Assertion failed [" + label + "]: expected true");
        }
    }

    private static void assertFalse(boolean cond, String label) {
        if (cond) {
            throw new AssertionError("Assertion failed [" + label + "]: expected false");
        }
    }
}
