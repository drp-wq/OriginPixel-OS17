# Build Environment & Reproducible Build-Host Preparation Plan

**Controller VPS:** `srv2115613` (`Ubuntu 24.04.5 LTS`, 2 vCPU, 7.8 GiB RAM, 0B swap, 33 GiB free disk)  
**Machine-Readable Audit:** [environment-audit-2026-11-22.json](file:///srv/originpixel-os17/docs/environment-audit-2026-11-22.json)

---

## 1. VPS Role Verdict (Phase 0 #2 & #3)

`srv2115613` **DOES NOT** meet Android 17 / LineageOS 24.0 full ROM compilation requirements:
- **RAM:** 7.8 GiB (0B swap) vs. **32–64 GiB required** for Soong/Ninja/R8/Clang.
- **Disk:** 33 GiB free vs. **450–600 GiB NVMe/SSD required** (source tree ~250 GB, `out/` ~150 GB, `ccache` 50–100 GB).
- **CPU:** 2 vCPUs vs. **16–32+ vCPUs recommended** for reasonable build times.

**Designated Role of `srv2115613`:**
- Primary Git controller & documentation hub (`/srv/originpixel-os17`)
- Local manifest & patch management
- CI/CD orchestrator & artifact metadata/checksum store
- Kotlin/Android app & unit test development (lightweight Gradle tasks only, when needed)
- **Preserved Services:** All trading services (`nexora-backend`, `qxp-app`, `qxp-ng-paper`, `nova-paper-bridge`, `neural-lab-*`, `gcx`) and unrelated projects remain untouched.

---

## 2. Dedicated Build Host Requirements (Gate G2)

To compile an unmodified LineageOS 24.0 (`mustang`) baseline (Gate G3) and subsequent OriginPixel OS 17 builds, provision a dedicated Linux x86_64 build host (cloud VM or bare-metal server):

| Resource | Minimum | Recommended |
| :--- | :--- | :--- |
| OS | Ubuntu 24.04 LTS x86_64 | Ubuntu 24.04 LTS x86_64 |
| CPU | 16 vCPUs | 32–64 vCPUs |
| RAM | 32 GiB (+ 16 GiB zram/swap) | 64 GiB RAM |
| Storage | 500 GB NVMe SSD | 1 TB NVMe SSD |
| Network | 1 Gbps unmetered | 1–10 Gbps |

## 3. Reproducible Build-Host Preparation Plan

1. **Provision Build Host** matching the table above.
2. **Run Bootstrap Script:** Copy and execute [prepare-build-host.sh](file:///srv/originpixel-os17/scripts/prepare-build-host.sh) on the build host. It:
   - Verifies RAM (>= 32 GiB) and free disk (>= 450 GiB) before proceeding.
   - Installs LineageOS 24.0 build dependencies (`bc`, `bison`, `build-essential`, `ccache`, `curl`, `flex`, `g++-multilib`, `gcc-multilib`, `git`, `git-lfs`, `gnupg`, `gperf`, `imagemagick`, `lib32readline-dev`, `lib32z1-dev`, `libelf-dev`, `liblz4-tool`, `libsdl1.2-dev`, `libssl-dev`, `libxml2`, `libxml2-utils`, `lzop`, `pngcrush`, `rsync`, `schedtool`, `squashfs-tools`, `xsltproc`, `zip`, `zlib1g-dev`, `python-is-python3`).
   - Installs Google's `repo` tool into `~/bin/repo`.
   - Configures `ccache` (`50G`–`100G`).
3. **Sync LineageOS 24.0 Source + Local Manifest:**
   - `repo init -u https://github.com/LineageOS/android.git -b lineage-24.0 --git-lfs`
   - Copy [originpixel_mustang.xml](file:///srv/originpixel-os17/manifests/originpixel_mustang.xml) to `.repo/local_manifests/originpixel_mustang.xml`.
   - `repo sync -c --no-clone-bundle --optimized-fetch --prune -j$(nproc)`
4. **Verify Clean Baseline (Gate G3):**
   - `source build/envsetup.sh`
   - `breakfast mustang`
   - `brunch mustang` (or `m bacon`)
   - Record build logs, `sha256sum` of output images/zip, and `repo manifest -r` pinned snapshot.
