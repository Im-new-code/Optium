# OptiLithium companion for Optium (Minecraft 1.17.1)

This experimental companion repackages **Lithium 0.7.5** for Minecraft 1.17.1 so it can be tested with Optium, the existing OptiFine bridge. It is separate from the future Optium+ renderer project.

## What was learned from the supplied OptiLithium JAR

The uploaded OptiLithium-fabric-mc1.19.4-0.11.1.jar identifies itself as Lithium 0.11.1, Minecraft 1.19.4, with mod ID `lithium`. Its embedded source URL currently returns 404. The clear compatibility-specific loader change is removal of upstream Lithium's `breaks: { "optifabric": "*" }` entry. This 1.19.4 artifact cannot be used on Minecraft 1.17.1, so the companion is generated from the upstream 1.17.1 release instead.

Official target source: https://github.com/CaffeineMC/lithium/tree/mc1.17.1-0.7.5
Official release details: https://modrinth.com/mod/lithium/version/FHFKMKeu
Source audit: [OPTILITHIUM_SOURCE_AUDIT.md](OPTILITHIUM_SOURCE_AUDIT.md)

## Exact modifications

The packaging script makes only metadata changes:

- Keeps mod ID lithium, Minecraft/Fabric Loader requirements, mixin lists, access widener, all Lithium classes, mixin resources, and the LGPL-3.0-only license.
- Removes only `breaks.optifabric` to mirror OptiLithium's original compatibility-package approach.
- Renames the displayed mod to OptiLithium for Optium, gives the repackaged build its own `0.7.5-optium.1` version string, attributes the compatibility port, and points source/issues links to this repository.
- Adds `OPTILITHIUM-OPTIUM.txt` containing the upstream version/source and SHA-256 provenance.

No Lithium class file or mixin group is disabled in this first port. Lithium 0.7.5 does not ship the properties file found in later releases, so the packager does not copy newer-version settings into the older JAR. The provided OptiLithium 0.11.1 configuration's false values match documented defaults for that upstream version; they are not proven OptiLithium-specific patches.

**Important:** Optium has mod ID `optium`, not `optifabric`. Therefore `breaks.optifabric` is not itself a loader conflict when running Optium without OptiFabric. Removing it mirrors the original package, but does not by itself fix a deeper runtime Mixin collision. The supplied OptiFine HD U H1 source archive also shows an overlap around `GoalSelector.tick()`: OptiFine uses `CollectionUtils` there, while Lithium 0.7.5 overwrites that method. H1 is not the exact H2 pre1 build, and this overlap is a test risk, not proof of failure. See the source audit before inferring a patch.

## Build and verification

GitHub Actions downloads the official Lithium 0.7.5 artifact, creates the repackaged companion, then verifies:
- correct Lithium ID and 1.17.1 version target;
- removed `breaks.optifabric` and preserved loader/game dependencies;
- byte-for-byte equality for every original .class file;
- every original ZIP entry except `fabric.mod.json` remains unchanged;
- retained license and unchanged mixin / optimization resources;
- provenance file is the only added entry.

## Install / test notes

1. Use a duplicate Pojav profile for the first test. Keep the known-working Optium performance JAR/profile unchanged.
2. Put the generated OptiLithium JAR in that profile's `mods` directory alongside the Optium performance JAR, Fabric API, ReplayMod, and your separately supplied OptiFine build.
3. Remove any official Lithium JAR and any other OptiLithium JAR from that profile. Both use mod ID `lithium`, so installing two causes a duplicate-mod conflict.
4. Do not load OptiFabric alongside Optium.
5. Verify startup, entering/reloading a world, block entities/hoppers, shader loading, ReplayMod recording, and video rendering. If it crashes, remove the companion JAR to return to the previous known-good setup.

CI verifies package integrity, not in-game behavior. The candidate needs a runtime test on the target OPPO A3s/PojavLauncher/MobileGlues setup before it can be called runtime-compatible.
