# OriginPixel OS 17 — Architecture

## 1. Layered Modular Architecture

OriginPixel OS 17 is structured into four independently testable layers built on top of **LineageOS 24.0 (`lineage-24.0`)**:

1. **L1 — Resources & Design System (`overlays/`, `assets/`)**
   - Runtime Resource Overlays (RROs) strictly for overlayable resources.
   - Original translucent material tokens, dynamic Monet/wallpaper palettes, original SVG vector icons, typography/spacing scales, light/dark modes, original wallpapers, and boot animation.
2. **L2 — Launcher & Apps (`launcher/`)**
   - Home screen, smart folders, dock, customizable grid, widgets, recents integration, and Settings customization / Theme Manager app.
3. **L3 — Native SystemUI & SmartIsland (`systemui/`)**
   - Native SystemUI integration for SmartIsland, translucent Control Center, redesigned notification shade, lock screen, and AOD.
   - **SmartIsland Prototype Migration:** Incorporates proven pure-Kotlin domain logic from `/srv/pixel-island-pro` (`IslandCoordinator` priority queue: `CALL > TIMER > AIRPODS > CHARGING > MEDIA > NOTIFICATION`, `TimerEngine`, `IslandSettings` privacy/reduced-motion flags) into native SystemUI controllers, replacing foreground overlay hacks with proper SystemUI window/status-bar integration.
4. **L4 — Motion & Framework (`motion/`)**
   - Targeted, minimal framework/SystemUI motion hooks: interruptible spring physics, velocity-sensitive gestures, surface morphing, configurable haptics, and strict `Settings.Global.ANIMATOR_DURATION_SCALE` / reduced-motion compliance.

## 2. Clean-Room & Security Principles

- **100% Original Assets & Code:** Zero vivo OriginOS proprietary APKs, fonts, icons, or decompiled code.
- **Verified Boot & Security:** Never weaken SELinux, AVB, or Android privacy indicators for visual features.
- **Signing Isolation:** Production release keys are never stored on `srv2115613` or untrusted CI runners.
