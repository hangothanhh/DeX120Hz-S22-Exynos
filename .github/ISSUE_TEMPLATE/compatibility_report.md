---
name: Hardware Compatibility Report
about: Share real-hardware test results for an external monitor, USB-C cable/hub, or S22 ROM build
title: "[Compat] <Monitor / Cable or Hub> @ <Resolution & Hz>"
labels: compatibility
assignees: ''
---

> [!NOTE]
> Only submit results you **personally verified on real hardware** backed by evidence (`Monitor OSD`, `dumpsys display`, `dmesg`, or `TestUFO`). Do not submit guesses. (English or Vietnamese welcome.)

## 1. Device & OS
- **Device model:** (e.g., `SM-S901B`)
- **Android / One UI:** (e.g., `Android 12 / One UI 4.1`)
- **Root / LSPosed stack:**

## 2. Monitor & Connection
- **Monitor model (per OSD / e.g., `V2203H`):**
- **Connection type:** [Direct USB-C → DP/HDMI cable (4-lane) / USB 3.0 hub (2-lane)]
- **Maximum monitor resolution & refresh rate:**
- **Test Mode (`dex120_force_virtual_120`):** [Enabled / Disabled]
- **Phone screen state:** [AMOLED Black Screen / Awake normally / Off via power button]

## 3. Observed Measurements
- **Monitor OSD reported:** (e.g., `1920x1080 120Hz` or `75Hz`)
- **DeX Desktop in `dumpsys display`:**
- **Actual TestUFO / FPS app measurement:**
- **Suggested compatibility status:** [VERIFIED / PARTIAL / UNSUPPORTED]

## 4. Sanitized Evidence / Diagnostics
```text
(Paste 'Run Diagnostics' output with sensitive identifiers redacted)
```
