package me.modmuss50.optium.compat.charm.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.types.Type;

import net.minecraft.util.Util;

import me.modmuss50.optium.compat.InterceptingMixin;
import me.modmuss50.optium.compat.Shim;

@Mixin(Util.class)
@InterceptingMixin("svenhjol/charm/mixin/UtilMixin")
abstract class UtilMixin {
	@Inject(method = "getChoiceTypeInternal", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;debug(Ljava/lang/String;Ljava/lang/Object;)V", remap = false), cancellable = true)
	private static void optium_hookAttemptDataFixInternal(TypeReference type, String id, CallbackInfoReturnable<Type<?>> call) {
		hookAttemptDataFixInternal(type, id, call);
	}

	@Shim
	private static native void hookAttemptDataFixInternal(TypeReference type, String id, CallbackInfoReturnable<Type<?>> call);
}