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
 * OptiFine-specific replay hand compatibility. Some OptiFine paths may not
 * reach this callback; GameRendererReplayHandWindowMixin provides the scoped
 * renderer-level fallback for those paths.
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

        if (camera == null || interactionManager == null || !optium$isReplayCamera(camera)) {
            return;
        }

        if (!renderingHand) {
            interactionManager.setGameMode(GameMode.SPECTATOR);
            return;
        }

        // The caller only checks whether the mode equals SPECTATOR.
        interactionManager.setGameMode(camera.isSpectator() ? GameMode.SPECTATOR : GameMode.SURVIVAL);
    }

    private static boolean optium$isReplayCamera(ClientPlayerEntity player) {
        for (Class<?> type = player.getClass(); type != null; type = type.getSuperclass()) {
            if (REPLAY_CAMERA_CLASS.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }
}
