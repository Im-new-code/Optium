# Optium 0.2.0 development plan

This branch is for the next development version. The 0.1.0-dev release remains the tested compatibility baseline.

## Diagnostic screen

- Keep a compact vanilla-style Optium Status screen available through the configurable `O` keybinding in-game.
- Do not add a title-screen button: the previous injection crashed during title-screen initialization in the OptiFine runtime.
- Show actionable state only: bridge initialization, detected OptiFine version, renderer compatibility/bypass state, ReplayMod workaround status, Minecraft/Fabric/Java versions, loaded-mod count, and Java heap use.
- Keep the copy-diagnostics action so users can report problems without manually transcribing log lines.
- Do not add an FPS counter or FPS measurement UI. Minecraft's F3 display, OptiFine's FPS option, and the launcher already provide FPS visibility.
- Keep the screen passive: no polling threads, third-party UI dependencies, forced garbage collection, or renderer modification.
- Display “not confirmed” rather than claiming the ReplayMod workaround succeeded when the target mixin was not removed.

## Performance investigation

The latest log's roughly 0.7-second bridge setup time is startup work and does not prove the cause of a sustained FPS drop. Do not advertise an FPS boost until there is evidence.

1. Reproduce an OptiFine vs Optium A/B test on the same device, world/scene, render settings, shader state, and recording state.
2. Compare frame pacing and CPU/GPU behavior using existing game/launcher tools and targeted logs, not a redundant Optium FPS overlay.
3. Separate steady-state rendering costs from startup/remapping and integrated-server tick delays.
4. Optimize only a demonstrated hot path. Keep OptiFine rendering behavior and ReplayMod recording/rendering as regression checks.
5. Keep default runtime overhead negligible and add diagnostic logging only when it can answer a concrete question.

## Compatibility guardrails

- Minecraft 1.17.1, Fabric Loader 0.16.14, and Java 17 remain the priority profile.
- OptiFine remains a separate user-supplied JAR.
- ReplayMod remains installed; only its conflicting jGui keyboard mixin is conditionally disabled by the tested workaround.
- Sodium/Iris and the OptiFine renderer remain separate profile choices. When Sodium or Iris is detected, the OptiFine renderer integration should be bypassed.
- Do not bundle third-party performance mods.
- CI success verifies compilation and packaged resources, not runtime correctness on the target Android device. Test the rebuilt JAR on PojavLauncher before calling the crash fixed.
