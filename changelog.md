### Version 2.1.8

* [Bug] - Fix commit `3562a62` that was wiped out by release 2.1.7
* [Chore] - Update dependencies
* [Bug] - Fix NightBloom File ID extraction regex following API change
* [Exp] - Add experimental support for multi-sourceset projects
* [Feat] - Add support for new snapshot version formats for CurseForge
* [Feat] - Add option to not set GitHub release as latest

### Version 2.1.7

* [Fix] - Find draft GitHub releases using tag [#26](https://github.com/firstdarkdev/modpublisher/pull/26)
* [Bug] - Modrinth Files are not listed in Debug output - [#23](https://github.com/firstdarkdev/modpublisher/issues/23)
* [Feat] - Experimental Support for Proxy Configuration - [#24](https://github.com/firstdarkdev/modpublisher/issues/24)
* [Bug] - Fix CurseForge Requests failing when content encoding header is null - [#28](https://github.com/firstdarkdev/modpublisher/issues/28)
* [Bug] - Remove Fractureizer Scanner cause ASM keeps causing issues with other plugins - [#29](https://github.com/firstdarkdev/modpublisher/issues/29)

### Version 2.1.6

* [FEAT] Support dependencies for NightBloom
* [DEV] More modloader remappings for the different platforms

### Version 2.1.5

* [FEAT] Add support for "draft" uploads for Modrinth and CurseForge - HypherionSA

### Version 2.1.4

* [FEAT] Add override setting for GitHub display name - [#21](https://github.com/firstdarkdev/modpublisher/issues/21) - HypherionSA

### Version 2.1.3

* [FIX] Support neoforge's `neoforge.mods.toml` file introduced in 20.5.0 - MattSturgeon

### Version 2.1.2

* [FIX] GitHub release `createTag` option was broken - [#15](https://github.com/firstdarkdev/modpublisher/pull/15) - MattSturgeon

### Version 2.1.1

* [FEAT] GitHub release `draft` and `target` options - [#11](https://github.com/firstdarkdev/modpublisher/pull/11) - MattSturgeon

### Version 2.1.0

* [BUG] Fixed Modrinth Uploads failing when using ModLoaders enum - HypherionSA
* [CHANGE] Deprecated `version` property, as it clashes with Gradle's version property. Replace with `projectVersion` - HypherionSA
* [FEAT] Support Gradle JavaVersions in CurseForge setJavaVersion - [#9](https://github.com/firstdarkdev/modpublisher/pull/9) - MattSturgeon
* [FEAT] Experimental GitHub release mutations - [#8](https://github.com/firstdarkdev/modpublisher/pull/8) - MattSturgeon
