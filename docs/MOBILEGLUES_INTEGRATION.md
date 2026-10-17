# MobileGlues integration notes for Optium

## Target environment

Optium's primary target is Minecraft 1.17.1 on Fabric, with OptiFine HD U H2 pre1, PojavLauncher, and the MobileGlues renderer on Android. MobileGlues is not an incidental compatibility layer in this setup; it has been present throughout the user's testing and must be treated as part of the target architecture.

MobileGlues is a GL implementation over host OpenGL ES 3.x and documents support for OptiFine shaders as well as Sodium/Iris. Its source includes a multi-draw backend selector with capability detection and fallbacks for native/extension multi-draw, indirect, multi-indirect, base-vertex, and compute-assisted paths. Which backend is actually selected depends on the MobileGlues version, driver symbols/extensions, and configuration. Do not assume every backend is active on every device.

## The critical distinction

MobileGlues can expose and emulate/dispatch modern GL entry points, but that does not by itself make OptiFine batch its terrain into multi-draw submissions. A renderer must create compatible command buffers, group compatible draws, maintain vertex/index-buffer and material state, and call the appropriate entry points. The wrapper can optimize or translate calls it receives; it cannot automatically restructure arbitrary OptiFine draw sequences into an optimal chunk renderer.

Therefore, Optium must not add a generic interception of every legacy GL call and assume it has produced an MDI pipeline. The integration question is specifically whether OptiFine's 1.17.1 terrain path, as transformed by Optium, reaches MobileGlues' multi-draw entry points with useful batches, and where CPU overhead remains.

## Revised investigation order

1. **Record the exact runtime:** MobileGlues version, Pojav renderer selection, OptiFine build, Optium JAR variant, shader, render distance, and MobileGlues multi-draw configuration. The user has already established that MobileGlues is in use; do not ask them to re-prove that.
2. **Inspect MobileGlues logs/configuration:** Determine which multi-draw backend is selected and whether capability fallback is occurring. Use MobileGlues' own logs/config rather than inferring support from a GL version string alone.
3. **Trace the OptiFine draw path:** Inspect the existing OptiFine patch/bytecode bridge and relevant call sites to find the actual terrain submission points. Determine whether draws are already batched before adding any hook.
4. **Separate CPU and GPU limits:** Compare vanilla and shader runs, use MobileGlues GPU reporting if available, and measure CPU time in Optium's bridge. A 12–20 FPS shader result may be GPU/shader-bound even if the CPU bridge is efficient.
5. **Prototype one change at a time:** If there is an Optium-side CPU hot path, optimize that first. If OptiFine submits unbatched draws and MobileGlues supports a compatible batch API, prototype a separate opt-in integration that uses existing buffer/state ownership and falls back cleanly. Do not invent a private MobileGlues Java API unless one is actually exposed.
6. **Retest compatibility:** Normal OptiFine rendering, Indigo custom models, resource packs, shaders, ReplayMod recording and final video rendering, world reload, and the target Android device.

## Consequences for proposed features

- **MDI:** Treat MobileGlues as a capable downstream GL layer. Investigate whether OptiFine already uses the available entry points and whether Optium can safely improve batching at the actual terrain submission layer. Do not reimplement MobileGlues' native dispatch or duplicate its fallback machinery in Java.
- **Asynchronous shadow maps:** MDI support does not imply that OpenGL work can be moved to arbitrary threads. Preserve the current render-context ownership. Only offload CPU-only preparation with immutable inputs and explicit synchronization.
- **Dynamic lighting and CTM:** These are game/OptiFine CPU data paths, not solved by the existence of MDI. Profile and cache only with correct world/chunk/resource invalidation.
- **Off-heap buffers:** MobileGlues' native backend does not prove that Optium's Java bridge allocates too much. Measure allocation and copies before adding another buffer pool.
- **Fallbacks:** Prefer capability detection before renderer setup. Do not attempt to recover from a failed Mixin application after transformed classes are already loaded.

## Current status

The current 0.3.0-dev branch's Indigo profiler is a diagnostic only. It measures time inside the Indigo block-model bridge and does not measure MobileGlues native draw-dispatch time, GPU shader time, or video encoding. Its measurements must be interpreted alongside MobileGlues GPU/backend information and matched gameplay tests. No FPS gain is claimed by this branch.
