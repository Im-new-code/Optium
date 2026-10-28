# ReplayMod first-person hand compatibility for OptiFine

## Symptom

During ReplayMod playback, the first-person hand and held item can disappear while spectating a recorded player, even though the hand works in normal gameplay and third-person replay view.

This matches the long-standing OptiFine issue [sp614x/optifine#6139](https://github.com/sp614x/optifine/issues/6139), which reports Minecraft 1.17.1 and ReplayMod 2.6.0. It is separate from Optium's jGui keyboard workaround.

## Fix

Optium registers optional compatibility hooks only when ReplayMod is present. It retains an OptiFine-specific hook for paths that invoke `Shaders.setRenderingFirstPersonHand(boolean)`.

On the tested OptiFine + ReplayMod + Pojav configuration, the shader callback was not reached. The proven fallback is a soft-targeted mixin around `GameRenderer.renderHand`. When ReplayMod's `CameraEntity` is attached to a recorded player while the interaction manager still reports `SPECTATOR`, the fallback temporarily changes the mode to `SURVIVAL` for the hand-render window, then restores the exact previous mode at the outermost return. Free-camera mode and ordinary gameplay are left unchanged.

The fallback uses a class-name check for ReplayMod's camera to avoid a hard compile dependency. Injection points use `require = 0` so the optional hook does not make unsupported targets fatal.

## Verification

- GitHub Actions compilation, packaged-JAR checks, resource validation, and performance-variant creation passed.
- Tested on Minecraft 1.17.1, Fabric Loader 0.16.14, OptiFine HD U H2 pre1, ReplayMod 2.6.20 and MobileGlues on Pojav.
- The hand and held item appeared correctly during ReplayMod playback with multiple shaderpacks.
- The main working branch and ReplayMod source were not modified by the fix branch.

## Third-party licensing

The optional OptiFine callback mixin adapts ReplayMod's workaround and is marked `GPL-3.0-or-later`. The full license text is packaged at `src/main/resources/META-INF/licenses/ReplayMod-GPL-3.0.txt`; see `THIRD_PARTY_NOTICES.md` for attribution.
