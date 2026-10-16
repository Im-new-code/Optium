# Optium + Minecraft 1.17.1 performance notes

Target profile: Minecraft 1.17.1, Fabric Loader 0.16.14, Java 17, PojavLauncher/Glow-Worm, MobileGlues, Adreno 506, and OptiFine HD U H2 pre1. OptiFine must remain a user-supplied separate JAR.

## What the latest log does and does not show

The log shows:

- `Found existing patched optifine jar, using that`: Optium reused its cache. The long first-run extraction/remapping process is not being repeated in this session.
- `Time elapsed: 33757 ms` while preparing a new world's spawn region, followed by integrated-server `Can't keep up` warnings of about 40, 315, 154, and 40 ticks behind in several bursts. This is world-generation/server-tick load, not an FPS counter.
- OptiFine scanned a large number of built-in connected-texture resources and repeatedly reloaded resources. That impacts startup/resource reload time more directly than steady-state FPS.
- A shaderpack is logged as loaded early in the run, but later the log says `[Shaders] No shaderpack loaded`. The log does not establish which shader state was active during the user's FPS measurement.
- Several external network requests fail because the device cannot resolve/reach Mojang or OptiFine hosts. Authentication, cape/skin lookup, and OptiFine version-check errors are not direct evidence of low rendering FPS.
- Warnings about missing `pack.mcmeta` occur for three ZIP resource packs. They should be fixed as resource-pack metadata, but they do not explain a sustained 30 FPS on their own.
- The log contains no FPS or per-frame-time samples. It cannot identify the exact cause of the reported 30 FPS.

Do a fair A/B test before attributing the drop to Optium: same world and position, same display resolution, render/simulation distance, graphics settings, resource packs and shader state. For a baseline, disable shaderpacks and confirm the shaders menu reports no shaderpack loaded. Collect an F3 screenshot or another frame-time/FPS sample over the same scene. New-world generation and first resource reload are poor steady-state FPS benchmarks.

## Renderer exclusivity: OptiFine vs Sodium/Iris

OptiFine is not just an optional performance patch: the bridge replaces and transforms core game classes. Sodium replaces the renderer, and Iris is built around Sodium. Disabling only a few OptiFine optimizers while still injecting its replacement renderer classes is not a safe compatibility strategy.

Optium now detects mod IDs `sodium` and `iris` in its early-riser setup. If either is present, Optium skips OptiFine discovery, class replacement and OptiFine-only mixins, and logs that OptiFine features/shaderpacks are inactive. This lets Sodium/Iris own rendering without attempting to run OptiFine's renderer on top. Use a separate OptiFine profile when you need OptiFine features; do not expect OptiFine shaderpacks to work in Sodium/Iris mode.

## Recommended separate mods

Add one mod at a time to a copied/test profile. These are separate Fabric mods, not embedded inside Optium. The versions below are the Minecraft 1.17.1-era releases, not whichever file is currently marked latest on the project page.

| Mod | Version/source for 1.17.1 | What it can help | Optium + OptiFine guidance |
|---|---|---|---|
| FerriteCore | [3.1.0 Fabric](https://www.curseforge.com/minecraft/mc-mods/ferritecore-fabric/files/3474756) | Reduces memory use; useful on a 3 GB Android device, but not a direct GPU/FPS boost | Best first memory test. Keep separate and test launch/world/resource reload. |
| Lithium | [0.7.4 for 1.17.1](https://www.curseforge.com/minecraft/mc-mods/lithium/files/3438799) | Game logic, mob AI and integrated-server work; relevant to the `Can't keep up` spikes in this log | Good CPU/server-tick experiment. It does not replace the renderer. Test parity and mod interactions. |
| LazyDFU | [0.1.2](https://www.curseforge.com/minecraft/mc-mods/lazydfu) | Defers data-fixer initialization, improving startup time | Low-risk startup optimization for 1.17.1; do not expect higher steady-state FPS. |
| Clumps | [7.0.8 Fabric 1.17.1](https://modrinth.com/mod/clumps/version/7.0.8) | Groups nearby XP orbs, reducing entity/tick load when many orbs are present | Useful for XP farms or orb-heavy areas; little benefit in an ordinary empty scene. Test separately. |
| Krypton | [Project / supported versions](https://modrinth.com/mod/krypton) | Optimizes packet/network handling | Primarily networking/server-side. Unlikely to raise local single-player rendering FPS on its own; optional for multiplayer testing. |
| Dynamic FPS | [3.4.2 for 1.17.1](https://www.curseforge.com/minecraft/mc-mods/dynamic-fps/files/5131299) | Reduces resource use while Minecraft is backgrounded/idle | A battery/background optimization, not a foreground FPS booster. |
| Mod Menu | [2.0.17 for 1.17](https://www.curseforge.com/minecraft/mc-mods/modmenu/files/4145216) | Shows installed mods and opens supported mod config screens | Convenience only; it is not a performance mod. |

## Do not add these to the first test

- **Entity Culling 1.5.0** has a historic 1.17.1 release, but its current compatibility notes mark the 1.16.5–1.19.2 line outdated/unsupported. It changes visibility and block/entity rendering, so test only after a clean baseline; do not assume compatibility with OptiFine.
- **Enhanced Block Entities 0.5 for 1.17.x** changes block-entity rendering. This overlaps OptiFine's rendering/model path, and its own documentation notes that mods changing block-entity rendering can break it. Leave it out of the initial OptiFine profile unless you are prepared to test chests, beds, signs, bells and shulker boxes carefully.
- **Noisium**: I could not verify a 1.17.1 Fabric release from the available project release information. Do not install a newer-version JAR into 1.17.1.
- **AI Improvements**: the 1.17.1 release is for Forge/NeoForge, not Fabric, so it is not suitable for this Fabric profile. Lithium already includes AI/game-logic optimizations for Fabric 1.17.1.

These labels describe purpose and risk, not a guarantee of OptiFine compatibility. None of the listed third-party mods has been tested here against your exact OptiFine build and complete Android profile.

## Suggested test order

1. Capture an OptiFine/Optium-only, no-shader baseline in the same world.
2. Add FerriteCore 3.1.0; retest launch, resource reload, and FPS.
3. Add Lithium 0.7.4; test world ticks and mob/redstone behavior.
4. Add LazyDFU 0.1.2; compare cold-start time, not FPS.
5. Add Clumps only if XP-orb-heavy areas matter.
6. Add Dynamic FPS and Mod Menu for convenience. Treat Krypton as optional and relevant mainly to networking.

Keep the OptiFine and Sodium/Iris profiles separate. If a mod introduces visual glitches or a crash, remove only that last-added mod and preserve the matching log.
