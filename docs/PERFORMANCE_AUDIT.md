# Performance-mod source audit for Optium 0.2.0

Target under review: Minecraft 1.17.1, Fabric Loader 0.16.14, OptiFine HD U H2 pre1, ReplayMod, PojavLauncher/MobileGlues, ARM64 Adreno 506.

## What source review says

### Optium's own startup pipeline
`OptifineSetup.getRuntime()` validates the OptiFine JAR/cache, remaps OptiFine classes and persists a gzip class cache. `OptifineInjector.setup()` replaces/patches class definitions as they are transformed/loaded. These operations occur at startup or class loading, not continuously in the rendered-frame loop. Shortening them should improve launch time, not raise a stable in-world FPS from 30–40 to 60+.

### Sodium
Sodium describes itself as a high-performance renderer replacement. Its documented optimizations include block-face culling, entity culling based on chunk visibility, texture animation limited to visible textures, chunk occlusion traversal, and improvements to chunk rendering/mesh submission. Those techniques are tightly coupled to its renderer and GPU state management. Sodium/Iris are already detected and bypass Optium's OptiFine replacement path. Mixing Sodium's renderer into OptiFine would defeat that separation and risks shader/ReplayMod regressions. Do not copy a renderer subsystem into this bridge as a patch-sized tweak.

### Lithium
Lithium's stated goal is minimally invasive game-logic optimization without changing gameplay behavior. Its patches can help game ticks, entities, and server-side work, but do not inherently optimize OptiFine's GPU-bound block rendering. A random subset port is not justified without a profile proving a matching hot path, and copying code would also require observing its LGPL-3.0 license.

### Entity Culling / particle culling / leaf culling
Entity culling can help scenes containing many entities or block entities hidden behind walls, but it is not a general renderer speedup. The official project currently marks the old 1.16.5–1.19.2 line as outdated/unsupported. Its 1.17.1 history also contains documented startup mixin failures. Its current license terms do not provide a safe blanket permission to port source code. OptiFine itself already has its own culling/animation/performance settings, so adding duplicate culling without proving a gap can add overhead or create rendering artifacts.

### Memory-focused mods
Memory deduplication and reduced allocation can help RAM pressure and garbage-collection pauses on the target phone. They do not establish that the observed steady FPS gap is a memory problem. A custom object-deduplication patch is not warranted without allocation/GC evidence.

## Hot paths found in Optium's renderer compatibility layer

- `optium.compat.indigo` adapts Fabric Indigo's model rendering to OptiFine's modified block-model renderer. Its block-model hook is on a hot path, but removing it indiscriminately can break Fabric custom models or cause mixin conflicts. It needs a compatibility-preserving benchmark/guard, not removal by assumption.
- `optium.compat.fabric-rendering` preserves Fabric's world-render/particle events. Its callback occurs in the render path but is not sufficient evidence of a large performance cost.
- `optium.compat.fabric-renderer-api` contains compatibility/reporting injections; some callbacks are on crash-report/error paths rather than every successful draw.
- `optium.optifine` includes the ReplayMod-neutral OptiFine fixes. The particle-color null guard is not a credible explanation for a 30 FPS loss.

## Decision

No speculative renderer rewrite, copied Sodium culler, or unsupported Entity Culling port is included. The status-screen/keybinding code was removed completely to eliminate the reported crash and avoid extra client tick work. The next legitimate performance patch must be backed by a controlled measurement that identifies which bridge hook accounts for the lost frame time.

## Required A/B method

Compare native OptiFine and Optium with identical render distance, simulation distance, graphics, clouds, entity distance, particles, shader state, resource pack, world, camera, and recording state. Warm up the scene and compare several stable 30-second windows after the phone reaches similar temperatures. Record average FPS plus whether the FPS changes when looking at a blank wall, a dense block scene, foliage, and many entities. Repeat with the same minimal mod list to isolate Fabric API/ReplayMod overhead. Do not call an optimization successful unless it raises the stable FPS or frame pacing without regressions in startup, world loading, shaders, or ReplayMod recording/rendering.

## Implemented experiment: separate renderer profiles

The CI build now stages two mutually exclusive JARs from the same verified code:

- **Compatibility profile** (`Optium-<version>.jar`): keeps `fabric-renderer-indigo:force_compatibility=true` and registers the existing Indigo compatibility hooks. Prefer this when mods require Fabric Renderer API / Indigo custom block models.
- **Performance candidate** (`Optium-performance-<version>.jar`): sets `fabric-renderer-api-v1:contains_renderer=true`, removes the Indigo force-compatibility flag, adds a marker resource, and skips Optium's Indigo interception mixins. This asks Fabric Renderer API to let OptiFine own terrain rendering, consistent with other OptiFine/Fabric integrations. It is a candidate, not yet a measured FPS win.

These variants must never be installed together because they share the same mod ID. The performance candidate deliberately trades away Indigo custom block-model compatibility. Standard vanilla terrain, OptiFine rendering/shaders, ReplayMod compatibility workaround, and non-renderer API integrations are still intended to work, but the exact mod list must be tested.

### Acceptance criteria for the performance candidate

1. It starts cleanly on the target device with Fabric Loader 0.16.14, OptiFine HD U H2 pre1, and ReplayMod 2.6.20.
2. It enters and reloads a world without missing chunks, invisible blocks, or model errors.
3. Shader packs load and ReplayMod can record and render with shaders.
4. Repeated matched-temperature A/B tests show better sustained frame rate or frame pacing than the compatibility JAR. If not, do not call it a performance improvement and return to profiling.
5. Mods relying on Indigo's custom rendering are documented as incompatible with this profile and can be recovered by switching to the compatibility JAR.

