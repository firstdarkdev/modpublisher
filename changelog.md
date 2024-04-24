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
