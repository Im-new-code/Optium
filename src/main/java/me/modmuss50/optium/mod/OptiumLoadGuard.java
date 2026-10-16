package me.modmuss50.optium.mod;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class OptiumLoadGuard implements PreLaunchEntrypoint {
	@Override
	public void onPreLaunch() {
		// Early Risers run before pre-launch. ReplayMod registers its
		// mixins.jgui.json config from its own Early Riser, so doing this in
		// OptiumSetup.run() is too early and cannot see the target config.
		if (FabricLoader.getInstance().isModLoaded("replaymod")) {
			OptiumSetup.disableReplayModJGuiKeyboardMixin();
		}
	}
}
