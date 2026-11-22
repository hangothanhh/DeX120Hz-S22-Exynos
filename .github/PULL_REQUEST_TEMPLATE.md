## 1. Summary of Changes
- **What changed:**
- **Why:**
- **Modified files:**

## 2. AI Disclosure & Anti-AI-Slop Checklist (Mandatory)
- [ ] I confirm I have read [CONTRIBUTING.md](../CONTRIBUTING.md) (including the AI Contribution & Anti-AI-Slop Policy).
- **AI involvement in this PR:** [ ] None / [ ] Partial AI assistance (reviewed line-by-line by me) / [ ] Mostly AI-generated (explain verification thoroughly below).
- **I understand every method/hook modified** and have not introduced unverified tweaks or fabricated compatibility claims.

## 3. Verification Completed
- [ ] `build.ps1` or `build.sh` builds cleanly without errors.
- [ ] `apksigner verify --verbose out/DeX120Hz-S22.apk` reports `Verifies`.
- [ ] `python scripts/verify_apk.py` (including `SanitizerTest`) passes 100%.
- [ ] Verified on physical hardware (required if modifying runtime hooks/UI):
  - **Device & ROM:** (e.g., `SM-S901B, Android 12 / One UI 4.1`)
  - **Root / LSPosed stack:**
  - **External monitor / test scenario:**

## 4. Screenshots or Sanitized Logs (Optional)
```text
(Confirm Serial, IMEI, MAC, private IPs, and Tokens are removed)
```

## 5. Compatibility & Rollback Plan
- **Compatibility impact:** (any changes to LSPosed scopes, `Settings.Global` keys, or `minSdk`?)
- **Rollback steps if regression occurs:**
