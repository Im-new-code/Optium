# ReplayMod first-person hand fix for OptiFine

## Symptom

In a ReplayMod replay, first-person spectating can show the scene but omit the spectated player's hand and held item. Normal gameplay and third-person spectating can still look correct. This is a known OptiFine/ReplayMod interaction on Minecraft 1.17.1, not a terrain-renderer or MobileGlues issue.

## Root cause

ReplayMod uses `com.replaymod.replay.camera.CameraEntity` as the player entity while a replay is open. The client interaction manager remains in spectator mode while ReplayMod uses the camera entity to represent the spectated player. OptiFine's shader hand-render path performs its own game-mode check, so it can suppress the first-person hand even when ReplayMod's render-hand option is enabled.

## Fix

When both ReplayMod and the OptiFine bridge are active, Optium registers an optional mixin on `net.optifine.shaders.Shaders.setRenderingFirstPersonHand(boolean)`. At the beginning of OptiFine's hand-render interval, it temporarily mirrors the ReplayMod camera entity's spectator state into the interaction manager. At the end, it restores spectator mode.

The fix:
- does not change normal gameplay or third-person player rendering;
- does not force hands visible when the spectated entity is a spectator;
- does not add a per-tick hook or rendering loop;
- does not require a compile-time dependency on ReplayMod;
- is registered only in the OptiFine profile when ReplayMod is detected.

The small compatibility mixin is adapted from ReplayMod's upstream `Mixin_ShowSpectatedHand_OF`, licensed GPL-3.0-or-later. Attribution and the license text are included in the JAR.

## Validation

GitHub Actions checks resource metadata and builds the Optium normal/performance JARs. This is not a substitute for testing the actual runtime. On the target profile, verify:
1. normal gameplay still renders the held item and hand;
2. third-person replay remains unchanged;
3. first-person replay while spectating a survival/adventure player shows the hand and held item;
4. first-person replay while spectating a spectator does not incorrectly force a hand;
5. ReplayMod video rendering still works, with and without an OptiFine shader pack.

Target setup: Minecraft 1.17.1, Fabric Loader 0.16.14, OptiFine HD U H2 pre1, ReplayMod 2.6.20, PojavLauncher and MobileGlues 2.0.0.
