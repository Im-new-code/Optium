# Optium 0.2.0 development plan: performance and compatibility

Optium's purpose is to bridge OptiFine into Fabric with maximum compatibility and minimal runtime overhead. The crashing status screen, keybinding, and per-tick hook have been removed. There is no Optium FPS overlay. Mod Menu remains optional and is not required.

## Separate renderer profiles

The build stages two mutually exclusive JARs from the same source:

- **Compatibility profile** (`Optium-<version>.jar`) keeps Indigo compatibility mode and Optium's existing Indigo adapter hooks. Use this when mods need Fabric Renderer API / Indigo custom block-model rendering.
- **Performance candidate** (`Optium-performance-<version>.jar`) declares `fabric-renderer-api-v1:contains_renderer=true`, removes `fabric-renderer-indigo:force_compatibility`, and skips Indigo interception mixins, so OptiFine owns terrain rendering. This avoids the per-block Indigo bridge, but may break mods that depend on Indigo's custom rendering. It is an experiment and is not yet a measured FPS win.

Both JARs use the same mod ID and must never be installed together. Start with the compatibility build for broadest mod support. Test the performance candidate in a duplicate Pojav profile and switch back if custom models disappear or any shader/ReplayMod regression appears.

## Performance findings

- OptiFine remapping, class-cache validation, ASM class replacement setup, and transformation registration happen at startup/class loading. They do not, by themselves, explain a sustained in-world FPS drop.
- Sodium's large performance gains come from its renderer architecture, including block/mesh culling and visibility handling. Copying its renderer subsystem into OptiFine is not a safe mixin-sized optimization.
- Lithium primarily optimizes game logic; entity culling and memory mods help only when their targeted workload is the bottleneck.
- The Indigo adapter in `optium.compat.indigo` is a plausible hot path because it hooks block-model rendering. The performance candidate removes that path through a separate renderer-ownership profile rather than trying to guess inside every block render.
- No stable FPS increase is claimed until repeated A/B tests confirm it on the target device.

## Required validation

1. Keep Minecraft 1.17.1, Fabric Loader 0.16.14, Java 17, PojavLauncher/MobileGlues as the primary target.
2. Test each profile with identical world, camera position, render/simulation distances, graphics settings, resource packs, shader state, and mod list.
3. Compare steady 30-second windows after scene warm-up and similar device temperature. Try a blank wall, dense terrain, foliage, and entities.
4. Test world entry/reload, shaders, and ReplayMod recording plus video rendering.
5. Accept the performance candidate only if it improves stable FPS/frame pacing without crashes or unacceptable compatibility regressions.
6. Keep the tested ReplayMod keyboard-mixin workaround unchanged.
7. Keep Sodium/Iris separate from the OptiFine profile. Do not bundle third-party performance mods or add background polling/forced GC.
