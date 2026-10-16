# Optium 0.2.0 performance and compatibility plan

Optium's purpose is to bridge OptiFine into Fabric with maximum compatibility and minimal runtime overhead. There is no Optium status screen, keybind, HUD, or FPS overlay. Keep Mod Menu optional and do not add configuration screens unless a proven optimization requires a user-facing setting.

## Performance findings so far

- OptiFine remapping, class-cache validation, ASM class replacement setup, and transformation registration are startup/class-loading operations. They do not, by themselves, explain a sustained 30–40 FPS result after the world is loaded.
- Sodium's performance lead is primarily architectural: it replaces parts of Minecraft's chunk/block renderer and improves visible geometry submission, chunk meshing, and visibility handling. Those hooks cannot be safely copied into OptiFine's renderer as isolated mixins without risking shaders, Fabric Rendering API behavior, and ReplayMod.
- Some broadly used optimizations (entity culling, particle culling, block-entity rendering changes, memory deduplication, and game-tick optimizations) overlap with existing OptiFine features or target a different bottleneck. Do not add a second culling path merely because it helps under vanilla/Sodium.
- The title-screen version text runs only on the title screen and is not an explanation for reduced FPS while in a world. The status-screen code and tick/keybinding hook have been removed rather than expanding this UI.

## Optimization process

1. Profile Optium's render path and compatibility mixins on the exact Minecraft 1.17.1 + OptiFine HD U H2 pre1 profile. Compare with normal OptiFine using identical world, camera position, render distance, graphics options, shader state, resource pack, and recording state.
2. Separate CPU-bound chunk building, CPU-bound render submission, GPU fill/shader load, integrated-server tick stalls, and thermal throttling. Use existing F3/launcher FPS displays and logs; do not add an Optium FPS overlay.
3. For each proposed patch, identify its target hot path and the expected bottleneck. Add no-op overhead neither to the render loop nor to each game tick.
4. Reuse external mod code only when its license permits it and the exact hook is compatible with this OptiFine target. Prefer porting a narrow, independently testable idea over copying a whole renderer subsystem.
5. Test every candidate for startup, world loading, ReplayMod record/render, shaders, and a renderer-free control. Reject any patch that harms the baseline or produces graphical artifacts.

## Compatibility guardrails

- Priority target: Minecraft 1.17.1, Fabric Loader 0.16.14, Java 17, PojavLauncher/Android.
- OptiFine remains a separate user-supplied JAR.
- Keep the tested ReplayMod keyboard-mixin workaround.
- Sodium/Iris and OptiFine remain separate renderer profiles. When Sodium or Iris is detected, Optium must bypass OptiFine class replacement and OptiFine-specific mixins.
- Do not bundle third-party performance mods, require Mod Menu, introduce polling threads, or perform forced garbage collection.
- CI verifies compilation and resource/JAR structure; runtime behavior and FPS must be tested on the target Android device before claiming a performance improvement.
