# Optium

Optium is an independent compatibility bridge for Minecraft Java Edition 1.17.1, targeting Fabric and constrained Android devices.

## Project goal

Provide an OptiFabric-style integration path for a separately supplied OptiFine JAR without requiring OptiFabric itself to be installed. OptiFine is not bundled with this project.

## Current status

**Early development. Not yet a working OptiFine loader.** The repository is being initialized before porting the correct OptiFabric 1.17.1 implementation. Do not treat this repository or any bootstrap prototype as a functional compatibility bridge until a build is tested in Minecraft.

## Compatibility target

- Minecraft Java Edition 1.17.1
- Fabric Loader, with versions to be pinned to the selected 1.17.1 source revision
- Java 17 runtime on Android/PojavLauncher as the primary test environment
- OptiFine supplied separately by the user
- ReplayMod and other Fabric mods tested individually; compatibility is not assumed

## Development plan

1. Port the upstream OptiFabric implementation from a verified 1.17.1-compatible revision, preserving its license and attribution.
2. Rename the mod identity to Optium and remove any runtime or metadata dependency on OptiFabric.
3. Keep OptiFine as a separate user-provided JAR; implement clear discovery, validation, and diagnostics.
4. Establish a reproducible build and CI workflow.
5. Test startup and crash cases on Minecraft 1.17.1 before making compatibility claims.
6. Add a compact settings/diagnostics UI only after the loader path works.
7. Profile performance on low-memory Android devices before making optimization changes.

## Licensing

Optium's ported portions must retain the applicable upstream license and copyright notices. OptiFine is a separate project and is not included here. Check each dependency's license before redistributing it.

## Contributing

Please include the Minecraft version, Fabric Loader version, OptiFine build, mod list, device/renderer details, and latest log or crash report with compatibility reports.
