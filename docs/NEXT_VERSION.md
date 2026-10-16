# Optium 0.2.0 development plan

This branch is for the next development version. The 0.1.0-dev release remains the tested compatibility baseline.

## First implementation slice

- Add a small vanilla-style Optium Status screen accessible from the title screen and the configurable `O` keybinding.
- Show bridge mode, detected OptiFine version, Minecraft/Fabric/Java versions, ReplayMod workaround state, loaded-mod count, and Java heap use.
- Let users copy a compact diagnostic report without needing to manually transcribe log lines.
- Keep the UI passive: no polling threads, third-party UI dependencies, forced garbage collection, or changes to OptiFine rendering.
- Keep the tested ReplayMod workaround intact and display “not confirmed” rather than claiming success when the target mixin was not removed.

## Performance approach

The latest log's roughly 0.7-second bridge setup time is startup work and does not prove the cause of a sustained FPS drop. Do not advertise an FPS boost until we have controlled measurements.

Next performance work should be split into measurable tracks:
1. Reproduce an OptiFine vs Optium A/B on the same device, same world/scene, same render settings and shader state.
2. Profile steady-state frame time separately from startup/remapping and integrated-server tick delays.
3. Only optimize a demonstrated hot path; keep OptiFine rendering behavior and ReplayMod recording/rendering as regression checks.
4. Add optional diagnostic logging only where it provides actionable information, and keep the default runtime overhead negligible.

## Compatibility guardrails

- Minecraft 1.17.1, Fabric Loader 0.16.14 and Java 17 remain the priority profile.
- OptiFine remains a separate user-supplied JAR.
- ReplayMod remains installed; only its conflicting jGui keyboard mixin is conditionally disabled by the tested workaround.
- Sodium/Iris and the OptiFine renderer remain separate profile choices.
- Do not bundle third-party performance mods.


The 0.2.0 status screen now uses Minecraft 1.17.1's `onClose()` and `isPauseScreen()` lifecycle methods; CI is used to verify the implementation before runtime testing.
