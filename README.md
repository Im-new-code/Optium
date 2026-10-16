# Optium

![Optium](src/main/resources/assets/optium/icon.png)

**Optium is an independent Fabric-side compatibility bridge for running a separately obtained OptiFine JAR.** It is based on the MPL-2.0-licensed OptiFabric implementation and is being adapted and tested for Minecraft 1.17.1, with special attention to Fabric Loader 0.12.5 and Android/PojavLauncher.

> Development status: the OptiFine bridge has been boot-tested on Android with Minecraft 1.17.1 and Fabric Loader 0.16.14. Performance tuning and broader mod-combination testing remain in progress.

## Current engineering target

- Minecraft Java Edition **1.17.1**
- Fabric Loader **0.16.14** as the current Android target; the historical 0.12.x compatibility work is retained
- Fabric API 1.17.x, including the 0.43.x compatibility fixes present in the chosen upstream source revision
- OptiFine supplied separately by the user; Optium does not bundle OptiFine
- Android/PojavLauncher constrained-memory launches

The upstream source uses Minecraft 1.16.5 as its compile/remapping baseline while choosing runtime mappings and compatibility mixins for later game versions. Do not change that baseline casually: it is part of the upstream multi-version build design, not a claim that the runtime target is 1.16.5.

## Compatibility notes

The original 1.17.1-era loader incompatibility was real: earlier OptiFabric releases declared `fabricloader <0.12`, and affected builds called `getGameProvider().getGameContextJars()`, which could fail with `NoSuchMethodError`. The upstream [Loader 0.12 support commit](https://github.com/Chocohead/OptiFabric/commit/fc09c5188fd7b8968976df07e7286dcb0ab0c97e) raised the supported baseline to Loader 0.12.5 and replaced that call with a newer object-share lookup plus a fallback path for older APIs. The source port used by Optium includes that compatibility logic. Separate upstream commits address Fabric API 0.43 and 0.43.1+ compatibility. CI confirms the source builds and the JAR passes packaging checks, but it cannot prove that every mod combination launches correctly on PojavLauncher.

- [Upstream Loader 0.12 support](https://github.com/Chocohead/OptiFabric/commit/fc09c5188fd7b8968976df07e7286dcb0ab0c97e)
- [Upstream Fabric API 0.43 fix](https://github.com/Chocohead/OptiFabric/commit/48e35b149d9ed23e3f846344ab35a2623ad5b031)
- [Upstream Fabric API 0.43.1+ fix](https://github.com/Chocohead/OptiFabric/commit/9d6024e676a67518a8ed7f839360702017daaa57)
- [Upstream OptiFabric 1.13.16 source snapshot](https://github.com/Chocohead/OptiFabric/tree/5ed616735878b2a1da50d5df1e96888c34817bed)

## Build and verification

The GitHub Actions workflow checks metadata/resource consistency, builds with Java 17, then validates the remapped JAR's ZIP integrity, embedded Fabric-ASM dependency, entrypoint classes, logo bytes, and Java 8 class-file compatibility. A successful run uploads one distributable named `Optium-<version>.jar` in the `optium-mod-jar` artifact, avoiding source and development JAR variants. Download it from the workflow run's **Artifacts** section. These checks confirm packaging and bytecode structure, not that Minecraft has launched successfully.

On a phone, test using a backup profile first. Keep Optium, OptiFine, and Fabric API versions aligned with Minecraft 1.17.1. Do not load Optium and OptiFabric together. Optium detects Sodium/Iris in the early-riser phase and intentionally bypasses the OptiFine bridge, class replacements and OptiFine-only mixins so the external renderer can own rendering. In that mode, OptiFine features and shaderpacks are not active. Use separate profiles for OptiFine and Sodium/Iris.

For diagnostic class extraction, use the JVM argument -Doptium.extract=true. For any crash, attach the first relevant exception and the end of latest.log, not only a screenshot of the launcher.

## Performance approach

OptiFine's mapped class cache is retained to avoid repeating expensive extraction/remapping work on every launch. The fork avoids adding background threads or aggressive memory settings, which could make stability worse on low-memory Android devices. The current log shows the warm cache is reused; it also shows lengthy new-world spawn generation/server tick delays and resource-pack/shader transitions. It does not contain measured FPS or frame-time samples, so it cannot prove why the reported average is around 30 FPS.

Read [PERFORMANCE.md](docs/PERFORMANCE.md) for the log findings and a researched Minecraft 1.17.1 performance-mod matrix. Initial recommendations are FerriteCore, Lithium and LazyDFU as separate mods, tested one at a time. Renderer mods such as Entity Culling and Enhanced Block Entities need extra caution with OptiFine. No FPS gain is promised until a controlled same-scene comparison confirms it.

## Attribution and licensing

Optium modifies source from [Chocohead/OptiFabric](https://github.com/Chocohead/OptiFabric), originally by Modmuss50 and Chocohead. The inherited source remains under the Mozilla Public License 2.0; see [LICENSE](LICENSE). Required attribution and license notices are retained. OptiFine is a separate product and is not included here.
