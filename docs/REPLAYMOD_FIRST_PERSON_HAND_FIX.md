# ReplayMod first-person hand compatibility for OptiFine

## Symptom

During ReplayMod playback, the first-person hand and held item can disappear while spectating a recorded entity. The hand can still render in normal gameplay and in third-person replay view.

This matches the long-standing OptiFine issue [sp614x/optifine#6139](https://github.com/sp614x/optifine/issues/6139), which explicitly reports Minecraft 1.17.1 and ReplayMod 2.6.0. It is separate from Optium's existing jGui keyboard workaround.

## Why this patch exists

Newer ReplayMod source includes an OptiFine-specific workaround that accounts for OptiFine's shader renderer checking the current interaction/game mode during the first-person hand render pass. In a replay, the replacement camera entity can be treated as a spectator even when the camera is attached to a recorded entity and its hand should be visible.

Optium now registers a soft-targeted mixin only when ReplayMod is installed. It hooks OptiFine's `Shaders.setRenderingFirstPersonHand(boolean)` boundary and temporarily aligns the local interaction manager's mode with whether ReplayMod is in entity-spectating or free-camera mode. When OptiFine ends the hand pass, it restores spectator mode. The camera-class check is by name to avoid a hard compile dependency on ReplayMod.

Reference implementation in current ReplayMod source:
https://github.com/ReplayMod/ReplayMod/blob/stable/src/main/java/com/replaymod/replay/mixin/Mixin_ShowSpectatedHand_OF.java

## Safety and testing

- The mixin is not registered if ReplayMod is absent.
- It is a soft target of the separately supplied OptiFine shader class. The injection has `require = 0` so OptiFine builds without that hook do not fail at mixin application.
- The behavior is limited to ReplayMod's `CameraEntity`; normal player gameplay is untouched.
- Existing keyboard workaround and renderer-performance profile are unchanged.
- CI checks compilation and mixin metadata but cannot verify visual hand rendering. Confirm on Minecraft 1.17.1, OptiFine HD U H2 pre1, ReplayMod 2.6.20 and MobileGlues; test both free camera and spectating a recorded entity, along with shaders enabled/disabled. If the hook is not present or the hand remains invisible, capture `latest.log` from the affected session so the injection and target can be confirmed.

## Third-party licensing

The compatibility mixin adapts ReplayMod's OptiFine hand-rendering workaround and is marked `GPL-3.0-or-later`. The full license text is packaged at `src/main/resources/META-INF/licenses/ReplayMod-GPL-3.0.txt`; see `THIRD_PARTY_NOTICES.md` for attribution.
