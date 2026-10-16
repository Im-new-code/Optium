package me.modmuss50.optium.mod;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class OptiumLoadGuard implements PreLaunchEntrypoint {
	@Override
	public void onPreLaunch() {
		// This no-op entrypoint is retained to preserve the class-loading
		// ordering guard used by early-riser based launchers.
		// See ReplayMod's chain-loading rationale:
		// https://github.com/ReplayMod/ReplayMod/commit/27edfcb4f3cd0eac0c7fb24e87ee3fa67324ab0a
	}
}
