# Optium renderer architecture: safe optimization plan

Target: the OptiFine compatibility bridge for Fabric, with Minecraft 1.17.1 / Fabric Loader 0.16.14 / OptiFine HD U H2 pre1 as the primary test profile.

## Scope and engineering rule

Optium currently patches/remaps OptiFine classes and adapts selected Fabric APIs to OptiFine. It does not own a replacement chunk renderer, shader compiler, dynamic-light engine, CTM engine, or GPU command scheduler. A broad renderer rewrite cannot be implemented safely by adding a few injections around legacy OpenGL calls.

The first implementation in this branch is deliberately diagnostic: an opt-in timer around the existing Indigo block-model bridge. It does not change rendering behavior and is disabled unless the JVM property `optium.profile.indigo=true` is supplied. The purpose is to determine whether the bridge callback is a material CPU hot path before attempting to optimize or bypass it.

## Requested enhancements, feasibility and guardrails

### 1. Modern chunk submission / multi-draw indirect

**Not safe as a bridge-only change.** Multi-draw indirect requires ownership of compatible GPU buffers, vertex formats, draw grouping, synchronization, and state changes. Existing OptiFine terrain submission must be replaced or deeply integrated, not intercepted at arbitrary GL calls. It also must be feature-detected at runtime: OpenGL ES and translation layers do not guarantee desktop multi-draw-indirect behavior. A future prototype must be a separate renderer profile and fall back before renderer initialization, not midway through a frame.

### 2. Asynchronous shadow-map generation

**Do not move OpenGL shadow passes to arbitrary worker threads.** Minecraft/OptiFine rendering operates under context and GPU-state ownership constraints. Moving GL work off-thread without a deliberate shared-context and fence design risks races, deadlocks, or corrupted state. CPU-only visibility/geometry preparation can be asynchronous only where ownership and immutable inputs are explicit. Minecraft already rebuilds chunk geometry using its chunk-builder workers; profile before adding another scheduler.

### 3. Dynamic lighting

**Requires a measured hot path and ownership audit.** Light arrays are often tied to world/chunk lifecycle and must not be updated concurrently with rendering or world mutation. GPU offload requires a GPU-side representation and synchronization that OptiFine's current lighting code does not expose as a general extension point. First measure CPU cost and allocations in a reproducible dynamic-light scene; do not add background writes to existing arrays.

### 4. Connected textures (CTM)

**Chunk-build caching may be viable, but asset-load-only baking is not generally correct.** Connected-texture decisions can depend on neighbor block states, world position, resource-pack rules, and chunk boundaries. Baking only during resource loading lacks the world state. A safe experiment would precompute immutable per-chunk/per-face connection results during chunk mesh construction, invalidate on neighboring block updates and resource reloads, and retain the current CTM path as fallback. This requires access to OptiFine's runtime implementation and targeted tests for chunk edges, block updates, and resource packs.

### 5. Native / off-heap vertex pipeline

**Potentially redundant or risky until measured.** LWJGL/Minecraft renderers commonly already use native buffers for GPU uploads, so adding another off-heap layer may only add copies, lifetime bugs, and memory pressure. Profile allocation rate and identify which temporary objects are created by Optium's bridge versus OptiFine before changing buffer ownership. Any future pool must have bounded capacity, clear ownership, and explicit release on world unload/shutdown.

### 6. Compatibility failure handling and per-module options

Feature toggles are useful only when they gate a real, independently reversible module. The existing Indigo compatibility bridge cannot be disabled independently of Fabric's renderer ownership metadata after startup; that is why the compatibility and performance candidates are separate JARs with the same mod ID and must not be installed together. Runtime recovery from a failed Mixin application is not reliably possible once transformed classes have started loading. Fail-safe behavior must be designed before registering a configuration and retain the default bridge when preconditions are uncertain.

## First diagnostic: Indigo bridge timing

Run the compatibility JAR with the JVM argument:

```text
-Doptium.profile.indigo=true
```

After entering a world, wait for several log reports while looking at a representative scene. The report contains cumulative callback count, average callback duration, and maximum single-callback duration. Instrumentation is intentionally disabled by default and adds overhead when enabled. Do not compare its raw FPS against an uninstrumented run. Use the metrics to decide whether to investigate the Indigo bridge, then confirm any later optimization in an uninstrumented A/B test.

The performance candidate bypasses these Indigo hooks, so it will not produce this metric. This is by design; compare the compatibility profile's measured bridge cost with in-world frame pacing and the normal OptiFine/performance-candidate baselines.

## Implementation order

1. Keep the existing OptiFine class remapping/patching path and ReplayMod keyboard workaround unchanged.
2. Collect bridge timings with the opt-in diagnostic.
3. If the hook is a meaningful cost, propose one isolated optimization with a known compatibility fallback and CI validation.
4. Test vanilla terrain, Indigo-dependent models, resource packs, shaders, world reload, ReplayMod recording, and ReplayMod video rendering.
5. Attempt larger renderer work only as a separately owned prototype with explicit GPU capability checks; do not fold unvalidated renderer rewrites into the compatibility bridge.

No FPS improvement is claimed by this diagnostic change. The performance mode has so far matched normal OptiFine in user testing; shader FPS and render times must be evaluated separately from average vanilla FPS.
