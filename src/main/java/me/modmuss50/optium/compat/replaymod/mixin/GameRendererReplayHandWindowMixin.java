// SPDX-License-Identifier: MPL-2.0
package me.modmuss50.optium.compat.replaymod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.world.GameMode;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps OptiFine's first-person hand render from being suppressed when
 * ReplayMod is spectating a recorded player.
 *
 * Some OptiFine shader paths do not reach Shaders.setRenderingFirstPersonHand,
 * so this fallback temporarily adjusts the game-mode value around
 * GameRenderer.renderHand. It is limited to ReplayMod's CameraEntity and
 * restores the exact prior game mode when the outermost render call returns.
 */
@Pseudo
@Mixin(targets = "net.minecraft.client.render.GameRenderer")
abstract class GameRendererReplayHandWindowMixin {
    private static final String REPLAY_CAMERA_CLASS = "com.replaymod.replay.camera.CameraEntity";

    @Unique
    private static int optium$handRenderDepth;

    @Unique
    private static GameMode optium$previousGameMode;

    @Unique
    private static ClientPlayerInteractionManager optium$handRenderInteractionManager;

    @Unique
    private static boolean optium$changedGameMode;

    @Inject(method = "renderHand", at = @At("HEAD"), require = 0)
    private void optium$beginReplayHandWindow(CallbackInfo ci) {
        // Preserve the outer window across nested/overloaded renderHand calls.
        if (optium$handRenderDepth > 0) {
            optium$handRenderDepth++;
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity camera = client.player;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        if (camera == null || interactionManager == null || !optium$isReplayCamera(camera)) {
            return;
        }

        optium$handRenderDepth = 1;
        optium$handRenderInteractionManager = interactionManager;
        optium$previousGameMode = interactionManager.getCurrentGameMode();

        // ReplayMod's camera is non-spectator while attached to a recorded
        // entity. OptiFine can suppress the hand if its interaction manager
        // still reports spectator mode, so change only that specific case.
        optium$changedGameMode = !camera.isSpectator()
                && optium$previousGameMode == GameMode.SPECTATOR;

        if (optium$changedGameMode) {
            interactionManager.setGameMode(GameMode.SURVIVAL);
        }
    }

    @Inject(method = "renderHand", at = @At("RETURN"), require = 0)
    private void optium$endReplayHandWindow(CallbackInfo ci) {
        if (optium$handRenderDepth <= 0) {
            return;
        }

        optium$handRenderDepth--;
        if (optium$handRenderDepth != 0) {
            return;
        }

        ClientPlayerInteractionManager interactionManager = optium$handRenderInteractionManager;
        GameMode previousGameMode = optium$previousGameMode;
        boolean changedGameMode = optium$changedGameMode;

        // Clear state before restoring to prevent duplicate restoration.
        optium$handRenderInteractionManager = null;
        optium$previousGameMode = null;
        optium$changedGameMode = false;

        if (changedGameMode && interactionManager != null && previousGameMode != null) {
            interactionManager.setGameMode(previousGameMode);
        }
    }

    @Unique
    private static boolean optium$isReplayCamera(Entity entity) {
        for (Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            if (REPLAY_CAMERA_CLASS.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }
}
