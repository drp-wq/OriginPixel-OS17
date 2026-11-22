# Free Hosted Build Options Investigation — OriginPixel OS 17

**Date:** 2026-11-22
**Requirement for Full LineageOS 24.0 (`mustang`) Build:** Linux **x86_64**, **>= 32–64 GiB RAM**, **>= 500 GB NVMe SSD**, **3–6 hr** uninterrupted runtime.

---

## Evaluation Matrix (Zero Provisioning or Payment Performed)

| Free Hosted Option | Free Specs | Full LineageOS 24.0 Build Feasible? | Blocking Constraint |
| :--- | :--- | :---: | :--- |
| **GitHub Actions Free (`ubuntu-24.04`)** | 4 vCPU x86_64, 16 GiB RAM, ~14 GB free disk, 6 hr max | **NO** | Disk (~14 GB vs 500 GB) and RAM (16 GiB vs 32+ GiB) make AOSP sync/build impossible. **Ideal for Tier-1 unit + `kotlinc` stub CI.** |
| **Oracle Cloud Always Free** | Up to 4 OCPU **ARM64** (Ampere A1), 24 GiB RAM, 200 GB block | **NO** | ARM64 architecture unsupported by AOSP x86_64 prebuilts; 200 GB disk < 500 GB; 24 GiB RAM < 32 GiB. |
| **Google Cloud Free Tier (`e2-micro`)** | 2 vCPU shared, 1 GiB RAM, 30 GB disk | **NO** | Far below minimum RAM and disk. |
| **GitLab / CircleCI Free Tiers** | 2–4 vCPU, 4–8 GiB RAM, <= 25 GB disk | **NO** | Far below minimum RAM and disk. |

## Verdict
**No genuinely free hosted tier can compile a full Android 17 / LineageOS 24.0 ROM.**
- **Free Tier Role:** Keep GitHub Actions `ubuntu-24.04` for fast Tier-1 unit, schema, static-patch, and `kotlinc` stub compilation checks.
- **Full ROM Build Role (Gate G3):** Requires either a **local x86_64 workstation** (`>= 32–64 GiB RAM`, `>= 500 GB NVMe`, $0 recurring) or an **approved paid external cloud VM / bare-metal server** (see [G3_EXTERNAL_BUILD_HOST_RUNBOOK.md](G3_EXTERNAL_BUILD_HOST_RUNBOOK.md)).
