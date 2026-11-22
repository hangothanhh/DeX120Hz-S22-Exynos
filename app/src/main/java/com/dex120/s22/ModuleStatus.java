// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 hangothanhh

package com.dex120.s22;

public final class ModuleStatus {
    public static final String KEY_ENABLED = "dex120_enabled";
    public static final String KEY_BLOCK_DP_DEX = "dex120_block_dp_dex";
    public static final String KEY_BLOCK_TOKEN = "dex120_block_token";
    public static final String KEY_UNLOCK_MODES = "dex120_unlock_modes";
    public static final String KEY_SYNC_VIRTUAL = "dex120_sync_virtual";
    public static final String KEY_FORCE_VIRTUAL_120 = "dex120_force_virtual_120";
    public static final String KEY_TARGET_HZ = "dex120_target_hz";

    private ModuleStatus() {}

    // Self-hooked by HookEntry when LSPosed loads inside this app process.
    public static boolean isActive() {
        return false;
    }
}
