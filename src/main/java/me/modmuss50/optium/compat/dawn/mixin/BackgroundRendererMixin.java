package me.modmuss50.optium.compat.dawn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BackgroundRenderer.FogType;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;

import me.modmuss50.optium.compat.InterceptingMixin;
import me.modmuss50.optium.compat.Shim;

@Mixin(BackgroundRenderer.class)
@InterceptingMixin("com/hugman/dawn/mod/mixin/BackgroundRendererMixin")
abstract class BackgroundRendererMixin {
	@Redirect(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z", remap = true), remap = false)
	private static boolean optium_hasStatusEffect(LivingEntity entity, StatusEffect effect, Camera camera, FogType fogType, float viewDistance, boolean thickFog) {
		return dawn_hasStatusEffect(entity, effect, camera, fogType, viewDistance, thickFog);
	}

	@Shim
	private static boolean dawn_hasStatusEffect(LivingEntity entity, StatusEffect effect, Camera camera, FogType fogType, float viewDistance, boolean thickFog) {
		throw new AssertionError("Unexpectedly reached code path");
	}
}