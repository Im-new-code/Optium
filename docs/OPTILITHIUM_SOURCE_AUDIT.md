# OptiLithium source and compatibility audit

Target: Minecraft 1.17.1, Fabric Loader 0.16.14, Optium performance profile, OptiFine HD U H2 pre1, ReplayMod 2.6.20, PojavLauncher and MobileGlues.

## What the uploaded OptiLithium file shows

The supplied `OptiLithium-fabric-mc1.19.4-0.11.1.jar` declares:

- mod ID `lithium`, version `0.11.1`, Minecraft `1.19.4`;
- display name `OptiLithium`, author `PrinceNormal`, and contact/source links to `PrinceNormal/OptiLithium`;
- no `breaks.optifabric` entry;
- the normal Lithium mixin config and access widener, plus Lithium's default and dependency properties files.

The embedded source URL currently returns 404, so the original repository history cannot be inspected directly. The official CurseForge page describes the project as a modified Lithium build that allows it to load alongside OptiFabric, with PojavLauncher and low-end systems as its intended audience: https://www.curseforge.com/minecraft/mc-mods/optilithium

The visible loader-level change is removal of the upstream hard conflict `breaks: { "optifabric": "*" }`. The manifest's name, author, and contact values are packaging/attribution changes.

## What the configuration does and does not prove

The uploaded JAR's `lithium-mixin-config-default.properties` contains these values set to false:

- `mixin.ai.nearby_entity_tracking`
- `mixin.gen.cached_generator_settings`
- `mixin.world.block_entity_ticking.support_cache`
- `mixin.block.hopper.worldedit_compat`

For Lithium 0.11.1, the official configuration documentation also lists these as default-false settings. See the upstream branch's configuration reference: https://github.com/CaffeineMC/lithium/blob/mc1.19.4-0.11.1/lithium-mixin-config.md. Therefore, the values in the supplied JAR alone do **not** prove OptiLithium changed or added these settings for OptiFine compatibility.

This matters because Lithium 0.7.5 uses an older configuration system and does not ship the same default-properties file. In its source, `ai.nearby_entity_tracking` and `gen.cached_generator_settings` are initially enabled, while `world.block_entity_ticking.support_cache` is disabled. This port does not copy version-0.11.1 property files into Lithium 0.7.5 or silently change these defaults without evidence that OptiLithium itself made those changes.

## What changes in the 1.17.1 companion

The companion is based on the official Lithium 0.7.5 source/release for Minecraft 1.17.1. Its packaging script:

1. keeps the upstream Lithium code, mixin definitions, access widener, and license;
2. removes `breaks.optifabric` to mirror OptiLithium's published compatibility packaging approach;
3. adds clear Optium-port identity and provenance metadata.

CI checks the repackaged JAR against the exact upstream 0.7.5 input: original class files remain byte-identical, original entries are unchanged except `fabric.mod.json`, and the provenance file is the only new ZIP entry.

## Important Optium-specific finding

Optium declares mod ID `optium`, not `optifabric`. Consequently, the original Lithium `breaks.optifabric` rule is not, by itself, a loader conflict in a profile that uses Optium and has no OptiFabric JAR. Removing that entry mirrors the OptiLithium packaging method, but does not on its own resolve any runtime Mixin conflict between Lithium and OptiFine.

A static cross-check against the supplied OptiFine HD U H1 source archive found one concrete overlapping method to watch: OptiFine's `GoalSelector.tick()` uses its `CollectionUtils` helper, while Lithium 0.7.5's `GoalSelectorMixin` overwrites `tick()`. The supplied source archive is H1, not the exact H2 pre1 JAR, so this is a risk indicator rather than proof of an H2 failure. The original OptiLithium JAR does not provide source evidence of a special patch for that method. We therefore do not disable or rewrite it speculatively.

## Runtime validation still required

The build and package checks pass, but they do not run Minecraft or OptiFine. Test in a duplicate Pojav profile with the exact H2 pre1 build and current mod list. Check clean startup, title screen, world entry/reload, hopper and block-entity behavior, shader activation, ReplayMod recording, and video rendering. If any stage fails, capture `latestlog.txt` or the crash report; the first relevant exception/mixin failure is needed to decide the next targeted patch.

Do not install the official Lithium JAR and this companion together because both use mod ID `lithium`. Do not install OptiFabric alongside Optium.
