# OriginPixel OS 17 — Test Matrix

| Subsystem | Unit Tests | Lint / Static Analysis | Integration / Instrumentation | Perfetto / Frame Timing | Hardware Validation |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Build Host & Manifest** | Preflight script check | `xmllint` on local manifest | Clean LineageOS 24.0 `mustang` build (G3) | N/A | N/A |
| **L1 Design System (RROs)** | Token & contrast checks | Android Lint / AAPT2 | RRO overlayable verification | GPU overdraw / blur cost | G4+ (Auth Required) |
| **L2 Launcher** | Grid/folder state unit tests | Kotlin/ktlint + Android Lint | Compose/Espresso UI tests | App launch & scroll jank | G4+ (Auth Required) |
| **L3 SmartIsland** | `IslandCoordinator`, `TimerEngine`, `Privacy` | Kotlin/ktlint + Android Lint | SystemUI notification/media hooks | Morph frame pacing (<8.3ms @120Hz) | G4+ (Auth Required) |
| **L3 Shade / Control Center** | Tile & state unit tests | Android Lint | SystemUI shade tests | Pull-down jank & memory | G4+ (Auth Required) |
| **L4 Motion & Haptics** | Spring math & reduced-motion | Android Lint | Motion test harness | 120Hz LTPO jank & thermal | G4+ (Auth Required) |
