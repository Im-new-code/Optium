// SPDX-License-Identifier: MPL-2.0
package me.modmuss50.optium.compat.replaymod.mixin;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Diagnostic-only probe for the Minecraft hand-render entry point.
 *
 * This mixin does not cancel or otherwise change rendering. It is registered
 * only by Optium's optional ReplayMod hand diagnostic configuration.
 */
@Pseudo
@Mixin(targets = "net.minecraft.client.render.GameRenderer")
abstract class GameRendererHandPathDiagnosticMixin {
    private static final Set<String> optium$seenStates = new HashSet<>();

    @Inject(method = "renderHand", at = @At("HEAD"), require = 0)
    private void optium$traceHandRenderEntry(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        Entity cameraEntity = client.getCameraEntity();

        String playerClass = player == null ? "null" : player.getClass().getName();
        String cameraClass = cameraEntity == null ? "null" : cameraEntity.getClass().getName();
        String state = playerClass + "|" + cameraClass + "|" + (client.world != null);

        if (optium$seenStates.add(state)) {
            System.out.println("[Optium-HandFix-DIAG] GameRenderer.renderHand reached: player="
                    + playerClass + ", cameraEntity=" + cameraClass
                    + ", worldPresent=" + (client.world != null));
        }
    }
}
