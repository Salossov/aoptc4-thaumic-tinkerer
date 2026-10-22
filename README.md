# Thaumic Tinkerer

<img src="src/main/resources/logo.png" alt="Thaumic Tinkerer" width="128" height="128">

Unofficial port of **Thaumic Tinkerer 2.5-1.7.10-164** to **Minecraft 1.21.1 / NeoForge**, using the public AOP: Thaumcraft 4 API.

[Русский](README.ru.md) | [Attribution and license](NOTICE.md)

## Overview

The addon includes devices, foci, enchantments, infused crops and fire, golem control and ComputerCraft peripherals. KAMI content includes materials, equipment, awakened tools, wings, the pouch, Worldshaper's Seeing Glass, warp gates, Recall, Shadowbeam and the bedrock dimension.

Optional integrations are available for CC: Tweaked, Applied Energistics 2, Botania, EnderStorage, CB Multipart and an unofficial Blood Magic port. Tinkers' Construct, IndustrialCraft 2 and OpenComputers are not included. The legacy VersionChecker integration has been removed; a replacement update mechanism is planned.

Manual gameplay, visual and multiplayer acceptance remains in progress.

## Requirements and installation

| Component | Requirement |
| --- | --- |
| Minecraft | 1.21.1 |
| Java | 21 |
| NeoForge | 21.1.252 or newer within 21.1 |
| AOP: Thaumcraft 4 | Compatible 1.0.0 build |
| AOP: UI | Compatible 0.7.x build |
| Curios | 9.5.1+1.21.1 or newer within 9.x |

Install `ThaumicTinkerer-1.21.1-1.0.0.jar` and the required dependencies on both client and server. Every dependency must target Minecraft 1.21.1 / NeoForge. The separate Thaumcraft API JAR is a development dependency and must not be installed as a mod.

Core and addon builds must be compatible: this addon uses public contracts added during the port, so an older core artifact with the same version number may be insufficient. Update both components together.

Optional integrations require their own dependencies. **AOP:TC4 Compact** separately provides JEI/EMI recipes and Jade/WTHIT tooltips for Tinkerer devices. Recipe and tooltip viewers are not required by Tinkerer itself.

## Configuration

The AOP: UI settings interface opens from Mods, the Thaumonomicon gear or the pause menu. Client changes save immediately. Server changes save with **Save** or **Esc**; multiplayer editing requires OP level 4, while the singleplayer world owner can edit without cheats.

Restart-marked options apply after restart. Existing configuration files, keys and defaults are retained. Dependent controls preserve their individual saved values when a parent option disables them.

## Building from source

JDK 21 and compatible AOP: UI, Thaumcraft API and core runtime artifacts are required. Build the API and runtime from the core repository with `:api:assemble :mod:assemble`, supplying its `aopUiJar` property.

From this repository:

```powershell
$dependencies = @(
    '-PaopUiJar=C:/dependencies/aopui-1.21.1-0.7.0.jar'
    '-PthaumcraftApiJar=C:/dependencies/thaumcraft-api-1.0.0.jar'
    '-PthaumcraftRuntimeJar=C:/dependencies/thaumcraft-1.21.1-1.0.0.jar'
)
./gradlew.ps1 build @dependencies
```

On Linux and macOS, use `sh ./gradlew` with the same Gradle properties. JARs can alternatively be placed in `libs/` using the filenames declared in `build.gradle`.

Optional APIs are resolved from their publishers' Maven repositories. Development overrides are available through `botaniaJar`, `enderStorageJar`, `codeChickenLibJar` and `multipartJar`.

The gameplay artifact is `build/libs/ThaumicTinkerer-1.21.1-1.0.0.jar`. Verification code and third-party classes are excluded.

## Verification

`build` compiles verification sources and runs standalone data and presentation checks. Native GameTests run separately and require the core runtime through `thaumcraftRuntimeJar`.

Integration profiles use `withComputerCraft`, `withAppliedEnergistics`, `withBotania`, `withEnderStorage` and `withMultipart`, each set to `true`. The Blood Magic profile additionally requires explicit Blood Magic and Modopedia JARs.

Bug reports should include component versions, reproduction steps and the relevant log. Successful compilation does not establish complete gameplay, visual or multiplayer compatibility.

## Continuous integration

In **Actions → Build and release → Run workflow**, enable **release** to publish an ordinary GitHub release with tag `v<version>`. Leave **version** blank to use `mod_version` from `gradle.properties`, or enter `major.minor.patch` to override it for that run. The selected version is used in the JAR metadata, filenames and tag; the workflow does not modify the source files. Prerelease suffixes are rejected in release mode. Existing release tags are never overwritten. Pushes to `main` and manual runs with **release** disabled update the single `nightly` prerelease. Nightly JAR versions use `<mod_version>-nightly.<source_commit>`. Pull requests build artifacts without publishing. Local `act` runs never publish. Ordinary release notes list every commit since the nearest previous ordinary release on the selected history, with commit links and a comparison link. The first release includes the full history. Nightly notes omit this list.

Individual CI and **Build mod suite** use the same build recipes and `aop-suite-manifest.json` format. An individual workflow builds or reuses only its component and required dependencies, then publishes only its own component. The suite can import its JARs from that repository's Releases when source revision and contents, version, toolchain, recipe and dependency checksums match. Repository caches are independent; published JARs provide reuse across repositories. A nightly run accepts matching release or nightly artifacts; an ordinary release accepts only release artifacts, including dependencies. The `force_rebuild` input bypasses ready JARs and preserves Gradle caching. Keep `.github/scripts/suite.py` and `release.py` identical across the four repositories when updating the shared artifact contract.

For local `act` runs, place compatible dependency source checkouts at `.dependencies/aopui`, `.dependencies/thaumcraft` before running `act workflow_dispatch --input release=true`. Local runs use these checkouts and skip GitHub publication. For private dependency repositories, configure the optional `DEPENDENCY_TOKEN` secret with read access to those repositories.

The GitHub workflow runs for code and build changes. Documentation-only changes do not trigger a build.

| Repository variable | Purpose / default |
| --- | --- |
| `THAUMCRAFT_REPOSITORY` | Core source; `Salossov/aop-thaumcraft-4` |
| `THAUMCRAFT_REF` | Core revision; `main` |
| `AOPUI_REPOSITORY` | AOP: UI source; `Salossov/aop-ui` |
| `AOPUI_REF` | AOP: UI revision; `main` |

Builds must use compatible core and addon revisions. Private dependency checkouts require a token with repository read access.

## Credits and license

- [Vazkii](https://github.com/Vazkii/ThaumicTinkerer), pixlepix, nekosune and the original contributors - Thaumic Tinkerer.
- [Azanor](https://www.curseforge.com/minecraft/mc-mods/thaumcraft) - Thaumcraft.
- [Salossov](https://github.com/Salossov) - Minecraft 1.21.1 port and maintenance.

The original **CC BY-NC-SA 3.0** attribution and terms are preserved; see [NOTICE.md](NOTICE.md). The Tinkerer license does not grant permission to redistribute Thaumcraft itself. This project is unofficial.
