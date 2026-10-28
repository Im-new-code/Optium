// SPDX-License-Identifier: MPL-2.0
package me.modmuss50.optium.compat.replaymod.mixin;

import java.util.HashSet;
import java.util.Set;

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
 * Fallback for OptiFine runtimes where Shaders.setRenderingFirstPersonHand
 * is not reached by ReplayMod's first-person hand path.
 *
 * The interaction manager's game mode is temporarily changed only while a
 * ReplayMod CameraEntity is rendering its hand pass. A depth counter keeps
 * the mode stable through nested/overloaded renderHand calls. The original
 * mode is restored at the outermost return.
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

    @Unique
    private static final Set<String> optium$loggedStates = new HashSet<>();

    @Inject(method = "renderHand", at = @At("HEAD"), require = 0)
    private void optium$beginReplayHandWindow(CallbackInfo ci) {
        // Nested overloads/calls must keep the outer window's temporary mode.
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
        // entity. OptiFine may suppress the hand when the interaction manager
        // still reports spectator mode, so only change that exact case.
        optium$changedGameMode = !camera.isSpectator()
                && optium$previousGameMode == GameMode.SPECTATOR;

        if (optium$changedGameMode) {
            interactionManager.setGameMode(GameMode.SURVIVAL);
        }

        String state = camera.isSpectator() + "|" + optium$previousGameMode + "|" + optium$changedGameMode;
        if (optium$loggedStates.add(state)) {
            System.out.println("[Optium-HandFix-DIAG] GameRenderer hand window: camera="
                    + camera.getClass().getName() + ", cameraSpectator=" + camera.isSpectator()
                    + ", previousMode=" + optium$previousGameMode
                    + ", temporarilyChanged=" + optium$changedGameMode);
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

        // Clear the window state before restoring, so an unexpected callback
        // cannot cause a second restoration.
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
