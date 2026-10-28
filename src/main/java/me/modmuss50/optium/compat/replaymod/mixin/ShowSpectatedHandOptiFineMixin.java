// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from ReplayMod's OptiFine first-person hand workaround.
// See META-INF/licenses/ReplayMod-GPL-3.0.txt and docs/REPLAYMOD_FIRST_PERSON_HAND_FIX.md.
package me.modmuss50.optium.compat.replaymod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.world.GameMode;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * OptiFine's shader renderer performs its own hand-render checks. ReplayMod
 * uses a CameraEntity while viewing a replay, and the interaction manager
 * stays in spectator mode even when the spectated player should have a hand.
 *
 * OptiFine calls setRenderingFirstPersonHand(true) before drawing the hand and
 * false afterwards. Mirror the spectated camera's game mode for that interval
 * and restore spectator mode afterwards, matching ReplayMod's OptiFine fix.
 *
 * A string-based camera check deliberately avoids a hard compile-time
 * dependency on ReplayMod; this mixin is only registered when ReplayMod is
 * detected and Optium's OptiFine bridge is active.
 */
@Pseudo
@Mixin(targets = "net.optifine.shaders.Shaders", remap = false)
abstract class ShowSpectatedHandOptiFineMixin {
    private static final String REPLAY_CAMERA_CLASS = "com.replaymod.replay.camera.CameraEntity";

    @Inject(method = "setRenderingFirstPersonHand", at = @At("HEAD"), remap = false)
    private static void optium$syncReplayCameraGameMode(boolean renderingHand, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity camera = client.player;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;

        if (camera == null || interactionManager == null || !isReplayCamera(camera)) {
            return;
        }

        if (renderingHand) {
            // The exact non-spectator mode is unimportant here; OptiFine only
            // checks whether the game mode equals SPECTATOR.
            interactionManager.setGameMode(camera.isSpectator() ? GameMode.SPECTATOR : GameMode.SURVIVAL);
        } else {
            // Replay viewing uses spectator mode outside the hand-render pass.
            interactionManager.setGameMode(GameMode.SPECTATOR);
        }
    }

    private static boolean isReplayCamera(ClientPlayerEntity player) {
        for (Class<?> type = player.getClass(); type != null; type = type.getSuperclass()) {
            if (REPLAY_CAMERA_CLASS.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }
}
