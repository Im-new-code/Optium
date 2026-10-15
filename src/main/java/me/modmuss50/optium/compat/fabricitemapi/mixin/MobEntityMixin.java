package me.modmuss50.optium.compat.fabricitemapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;

import me.modmuss50.optium.compat.InterceptingMixin;
import me.modmuss50.optium.compat.PlacatingSurrogate;

@Mixin(MobEntity.class)
@InterceptingMixin("net/fabricmc/fabric/mixin/item/MobEntityMixin")
abstract class MobEntityMixin {
	@PlacatingSurrogate
	private void onGetPreferredEquipmentSlot(ItemStack stack, CallbackInfoReturnable<EquipmentSlot> info) {
		throw new AssertionError("Unexpectedly reached code path");
	}
}