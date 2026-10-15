package me.modmuss50.optium.compat.borderlessmining.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import net.minecraft.client.gui.screen.option.VideoOptionsScreen;

import me.modmuss50.optium.compat.InterceptingMixin;
import me.modmuss50.optium.compat.Shim;
import me.modmuss50.optium.compat.borderlessmining.MagicMixinBridge;

@Mixin(VideoOptionsScreen.class)
@InterceptingMixin("link/infra/borderlessmining/mixin/FullScreenOptionMixin")
abstract class VideoOptionsScreenMixin implements MagicMixinBridge {
	@Shim
	private native void modifyOption(Args args);

	@Override
	public void optium£modifyFullscreenButton(Args args) {
		modifyOption(args);
	}
}