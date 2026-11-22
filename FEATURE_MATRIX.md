# OriginPixel OS 17 — Feature Matrix

| Layer | Feature | Status | Target Gate | Verification Method |
| :--- | :--- | :--- | :--- | :--- |
| **L1** | Original translucent material & blur tokens | Local token spec | G5 | RRO build + screenshot regression blocked on G3 |
| **L1** | Dynamic wallpaper-based color palettes | Not implemented | G5 | Monet palette unit + UI test |
| **L1** | Original SVG icons, typography & spacing | Typography/spacing tokens | G5 | Vector lint + layout check remains |
| **L1** | Lock-screen themes, wallpapers & boot animation | Not implemented | G5 | Asset validation + boot check |
| **L2** | Custom home screen, smart folders, dock, grid | Framework-neutral state contract | G6 | Android launcher module, widgets and instrumentation blocked on G3 |
| **L2** | Widgets & recents integration | Planned | G6 | UI test + frame timing |
| **L2** | Settings customization & Theme Manager | Planned | G6 | Preference + RRO toggle tests |
| **L3** | Native SmartIsland (collapsed/expanded states) | Prototype in `pixel-island-pro` | G7 | Unit + SystemUI tests |
| **L3** | SmartIsland: Media, Calls, Timer, Charging, Privacy | Prototype in `pixel-island-pro` | G7 | Coordinator unit + integration |
| **L3** | Translucent Control Center & Notification Shade | Framework-neutral transition contract | G7 | SystemUI + accessibility tests blocked on verified G3 APIs |
| **L3** | Lock-screen interactions & AOD (LTPO 1–120Hz) | Planned | G7 | Power + AOD burn-in check |
| **L4** | Interruptible spring physics & surface morphing | Host spring integrator/spec | G8 | Motion harness passed; Perfetto jank blocked on G4 |
| **L4** | Configurable haptics & reduced-motion accessibility | SmartIsland host contract | G8 | Host regression passed; Android haptic validation blocked on G4 |
