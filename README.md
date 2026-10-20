# Thaumic Tinkerer

[Русский](README.ru.md)

I maintain an unofficial port of **Thaumic Tinkerer 2.5-1.7.10-164** to **Minecraft 1.21.1 / NeoForge**. It uses the public API of AOP: Thaumcraft 4.

## Content

The port includes the original devices, foci, enchantments, infused crops and fires, golem control and ComputerCraft peripherals. KAMI includes materials, equipment, awakened tools, wings, focus storage, the building mirror, warp gates, Recall, Shadowbeam and the bedrock dimension.

Optional integrations support CC: Tweaked, Applied Energistics 2, Botania, EnderStorage, CB Multipart and the unofficial Blood Magic port. Tinkers' Construct, IndustrialCraft 2 and OpenComputers are not included. Legacy VersionChecker integration is removed; a replacement updater is planned.

## Installation

Use Minecraft **1.21.1**, Java **21**, NeoForge **21.1.252 or newer**, **Curios 9.5.1+1.21.1 or newer** and the matching **AOP: Thaumcraft 4 1.0.0** build. All dependencies must target Minecraft 1.21.1 / NeoForge. Install the gameplay JAR on both client and server. Do not install the separate Thaumcraft API JAR as a mod.

The port requires the core's added public contracts, so an older core JAR with the same version number may be incompatible. Update the core and addon together.

Install optional mods with their own dependencies. **AOP:TC4 Compact** separately provides JEI/EMI recipes and Jade/WTHIT tooltips, including optional Tinkerer device details. Tinkerer itself does not require those viewers.

## Building

Install JDK 21. Build the API and runtime from the matching AOP: Thaumcraft 4 source:

```powershell
./gradlew.ps1 :api:assemble :mod:assemble
```

From this repository, pass the API JAR explicitly:

```powershell
./gradlew.ps1 build -PthaumcraftApiJar=/path/to/thaumcraft-api-1.0.0.jar
```

Alternatively place it in `libs/`. On Linux/macOS use `sh ./gradlew`. Optional APIs are downloaded from their publishers' Maven repositories. Explicit `-PbotaniaJar`, `-PenderStorageJar`, `-PcodeChickenLibJar` and `-PmultipartJar` overrides are available for development.

The gameplay artifact is `build/libs/ThaumicTinkerer-1.21.1-2.5.164-port.1.jar`. Tests and third-party classes are excluded from it.

## Verification

`build` compiles verification sources and runs standalone data and presentation checks. Native GameTests are separate and require a compatible core runtime via `-PthaumcraftRuntimeJar=/path/to/core.jar`. Optional integration profiles use `-PwithComputerCraft=true`, `-PwithAppliedEnergistics=true`, `-PwithBotania=true`, `-PwithEnderStorage=true` and `-PwithMultipart=true`. Blood Magic and Modopedia JARs must be supplied explicitly when enabling their profile.

Manual gameplay, graphics and multiplayer acceptance is ongoing. Report reproducible bugs with versions and relevant logs. The GitHub workflow builds code changes; documentation-only changes do not start a build.

## Credits and license

- [Vazkii](https://github.com/Vazkii/ThaumicTinkerer), pixlepix, nekosune and the original contributors — Thaumic Tinkerer.
- [Azanor](https://www.curseforge.com/minecraft/mc-mods/thaumcraft) — Thaumcraft.
- [Salossov](https://github.com/Salossov) — Minecraft 1.21.1 port, with AI-assisted implementation and verification.

I preserve the original **CC BY-NC-SA 3.0** attribution and terms. See [NOTICE.md](NOTICE.md). This license does not grant permission to redistribute Thaumcraft itself. This is an unofficial port.

## CI configuration

Repository variables `THAUMCRAFT_REPOSITORY` and `THAUMCRAFT_REF` select the core source (defaults: `Salossov/aop-thaumcraft-4`, `main`). Build against matching core and addon revisions. For private dependencies, the checkout workflow needs a token with read access.
