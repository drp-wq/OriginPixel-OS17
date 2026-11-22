# OriginPixel OS 17 — Risk Register

| ID | Risk | Severity | Mitigation | Status |
| :--- | :--- | :--- | :--- | :--- |
| **R-01** | VPS `srv2115613` has only 2 vCPU, 7.8 GiB RAM, 33 GiB free disk — attempting AOSP build here will OOM/fill disk and disrupt trading services. | **CRITICAL** | Strictly forbid AOSP sync/build on `srv2115613`. Enforce preflight RAM/disk guard in `scripts/prepare-build-host.sh`. Use dedicated build host for G2/G3. | **MITIGATED** |
| **R-02** | Physical Pixel 10 Pro XL is a production device with important apps (banking/trading). | **CRITICAL** | Hard gate at G4: never unlock, wipe, flash, or reboot without explicit user authorization. Document full stock Android 17 factory image rollback first. | **ACTIVE** |
| **R-03** | Banking & trading apps may fail Play Integrity (`MEETS_STRONG_INTEGRITY`) on unlocked bootloader / custom ROM. | **HIGH** | Explicitly document incompatibility; never assume banking/trading apps will work; never weaken Android security controls. | **ACTIVE** |
| **R-04** | Accidental deletion of legacy projects (`/srv/pixel-island-pro`, `/srv/pixel-aura-os`, `/srv/airpods-pixel-pro3`). | **MEDIUM** | Preserve all legacy directories until `legacy-pixel-projects-2026-11-22.tar.gz` is independently restored and verified. | **MITIGATED** |
| **R-05** | Exposure of signing keys or credentials in Git or cloud backups. | **HIGH** | Strict `.gitignore` blocking keys/secrets/archives; never store production signing keys on untrusted CI runners. | **MITIGATED** |
