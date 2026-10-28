// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from ReplayMod's OptiFine first-person hand workaround.
// See src/main/resources/META-INF/licenses/ReplayMod-GPL-3.0.txt and THIRD_PARTY_NOTICES.md.
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
 * OptiFine's shader renderer checks the active game mode while handling its
 * first-person hand pass. ReplayMod's camera entity can be treated as a
 * spectator during that check even when the replay is spectating an entity
 * whose hand should be visible. Temporarily synchronize the local interaction
 * manager's mode with the camera's hand-rendering state, then restore replay
 * spectator mode after the pass.
 *
 * This mixin is soft-targeted because the target class only exists when the
 * separately supplied OptiFine runtime is active.
 */
@Pseudo
@Mixin(targets = "net.optifine.shaders.Shaders", remap = false)
abstract class OptifineSpectatedHandMixin {
    private static final String REPLAY_CAMERA_CLASS = "com.replaymod.replay.camera.CameraEntity";

    @Inject(
            method = "setRenderingFirstPersonHand",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private static void optium$syncReplayCameraModeForHandPass(boolean renderingHand, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity camera = client.player;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;

        if (camera == null || interactionManager == null || !isReplayCamera(camera)) {
            return;
        }

        if (!renderingHand) {
            interactionManager.setGameMode(GameMode.SPECTATOR);
            return;
        }

        // ReplayMod's camera reports non-spectator while attached to a
        // recorded entity, but spectator while using the free camera.
        GameMode renderMode = camera.isSpectator() ? GameMode.SPECTATOR : GameMode.SURVIVAL;
        interactionManager.setGameMode(renderMode);
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
