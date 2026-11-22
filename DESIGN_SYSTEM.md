# OriginPixel OS 17 — Original Design System Specification (Draft)

> **Clean-Room Rule:** All visual tokens, icons, typography pairings, motion curves, and wallpapers are 100% original creations inspired by modern translucent glass/atomic UI aesthetics. Zero vivo OriginOS assets or fonts are used.

## 1. Material & Surface Tokens
- **Translucent Glass Levels:** `SurfaceGlassLow` (12dp blur, 78% alpha), `SurfaceGlassMedium` (24dp blur, 68% alpha), `SurfaceGlassHigh` (36dp blur, 58% alpha).
- **Fallback (Low-Power / Reduced Transparency):** Opaque tonal surfaces derived from Material You / Monet dynamic palette.
- **Corner Radii:** `RadiusPill` (999dp for SmartIsland), `RadiusCard` (24dp), `RadiusTile` (18dp), `RadiusSheet` (32dp).

## 2. Dynamic Color & Modes
- Full Light & Dark mode support anchored to Android 17 dynamic wallpaper color extraction.
- High-contrast text tokens meeting WCAG AA (>= 4.5:1 body, >= 3:1 large text).

## 3. Typography & Spacing
- Open-source variable sans-serif typography (system Roboto Flex / Inter-compatible metrics; no proprietary fonts).
- 4dp base spatial grid (`4, 8, 12, 16, 20, 24, 32dp`).

## 4. Motion & Accessibility
- Spring presets: `SpringSnappy` (stiffness 500, dampingRatio 0.82), `SpringFluid` (stiffness 320, dampingRatio 0.85), `SpringGentle` (stiffness 200, dampingRatio 0.90).
- Reduced Motion: Automatically degrades morphing/springs to fast crossfades when system reduced-motion is enabled.
