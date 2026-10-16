# Optium

![Optium](src/main/resources/assets/optium/icon.png)

**Optium is an independent Fabric-side compatibility bridge for running a separately obtained OptiFine JAR.** It is based on the MPL-2.0-licensed OptiFabric implementation and is being adapted and tested for Minecraft 1.17.1, with special attention to Fabric Loader 0.12.5 and Android/PojavLauncher.

> Development status: source port and build validation in progress. A successful compile does not by itself prove a successful launch on Android; device testing is still required.

## Current engineering target

- Minecraft Java Edition **1.17.1**
- Fabric Loader **0.12.5** as the historical compatibility baseline, with newer 0.16.x loaders as an additional target to test
- Fabric API 1.17.x, including the 0.43.x compatibility fixes present in the chosen upstream source revision
- OptiFine supplied separately by the user; Optium does not bundle OptiFine
- Android/PojavLauncher constrained-memory launches

The upstream source uses Minecraft 1.16.5 as its compile/remapping baseline while choosing runtime mappings and compatibility mixins for later game versions. Do not change that baseline casually: it is part of the upstream multi-version build design, not a claim that the runtime target is 1.16.5.

## Compatibility notes

A historical 1.17.1 report described a world-loading freeze with Loader 0.12.5 and Fabric API 0.43.0. The upstream maintainer identified a fabric-lifecycle-events-v1 mixin conflict. Upstream then added fixes for Fabric API 0.43 and 0.43.1+. Optium carries the later upstream source as its starting point; whether that fully resolves your exact mod list must be confirmed by a launch log.

- [Upstream Loader 0.12 support](https://github.com/Chocohead/OptiFabric/commit/fc09c5188fd7b8968976df07e7286dcb0ab0c97e)
- [Upstream Fabric API 0.43 fix](https://github.com/Chocohead/OptiFabric/commit/48e35b149d9ed23e3f846344ab35a2623ad5b031)
- [Upstream Fabric API 0.43.1+ fix](https://github.com/Chocohead/OptiFabric/commit/9d6024e676a67518a8ed7f839360702017daaa57)
- [Upstream OptiFabric 1.13.16 source snapshot](https://github.com/Chocohead/OptiFabric/tree/5ed616735878b2a1da50d5df1e96888c34817bed)

## Build and verification

The GitHub Actions workflow checks metadata/resource consistency, builds with Java 17, then validates the remapped JAR's ZIP integrity, embedded Fabric-ASM dependency, entrypoint classes, logo bytes, and Java 8 class-file compatibility. A successful run uploads one distributable named `Optium-<version>.jar` in the `optium-mod-jar` artifact, avoiding source and development JAR variants. Download it from the workflow run's **Artifacts** section. These checks confirm packaging and bytecode structure, not that Minecraft has launched successfully.

On a phone, test using a backup profile first. Keep Optium, OptiFine, and Fabric API versions aligned with Minecraft 1.17.1. Do not load Optium and OptiFabric together. OptiFine is not compatible with Sodium, so keep this in a separate profile from the Sodium/Iris setup.

For diagnostic class extraction, use the JVM argument -Doptium.extract=true. For any crash, attach the first relevant exception and the end of latest.log, not only a screenshot of the launcher.

## Performance approach

OptiFine's mapped class cache is retained to avoid repeating expensive extraction/remapping work on every launch. The fork avoids adding background threads or aggressive memory settings, which could make stability worse on low-memory Android devices. No FPS gain is promised: this project bridges OptiFine into Fabric; rendering performance depends on the device, OptiFine build, renderer, resource packs, and settings.

## Attribution and licensing

Optium modifies source from [Chocohead/OptiFabric](https://github.com/Chocohead/OptiFabric), originally by Modmuss50 and Chocohead. The inherited source remains under the Mozilla Public License 2.0; see [LICENSE](LICENSE). Required attribution and license notices are retained. OptiFine is a separate product and is not included here.
