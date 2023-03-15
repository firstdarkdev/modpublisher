## ModPublisher

A gradle plugin that allows you to publish mods to Curseforge and Modrinth in one go.

No more using two different plugins, when you can just use one.

This plugin is mostly aimed at projects using MultiLoader Template and for our internal use, but anyone is free to use it.

---

### Setup

To use this plugin inside your project, first you have to add our maven.

To does this, open up `settings.gradle` and add the following:

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url "https://maven.firstdarkdev.xyz/releases"
        }
    }
}
```

Next, in your `build.gradle` add:

![badge](https://maven.firstdarkdev.xyz/api/badge/latest/releases/me/hypherionmc/modutils/modpublisher?color=40c14a&name=modpublisher)

```groovy
plugins {
    id "me.hypherionmc.modutils.modpublisher" version "VERSION"
}
```

Replace VERSION with the version above.

Finally, add the following to `build.gradle` file:

```groovy
publisher {
    apiKeys {
        curseforge = System.getenv("CURSE_TOKEN") // Required if you want to use Curseforge Upload
        modrinth = System.getenv("MODRINTH_TOKEN") // Required if you want to use Modrinth Upload
    }

    debug = false // When enabled, no files will actually be uploaded
    curseID = 12345 // Curseforge Project ID
    modrinthID = xyn8677dh // Modrinth Project ID
    versionType = "release" // Release Type. Either release, beta, or alpha
    changelog = "changelog.md" // Changelog file, or text. Only MARKDOWN is supported
    version = "1.3.0" // Only used by modrinth
    displayName = "MyAwesome Mod - Version" // Friendly display name for the file
    gameVersions = ["1.19.3", "1.19.4"] // Supported Game Versions
    loaders = ["forge", "fabric", "quilt"] // Supported Modloaders
    artifact = jar // File or file location of the file to upload
}
```

Additional values that can be added to the above:

```groovy
curseDepends {
    required = ["fabric-api", "cloth-config"] // Required Dependencies
    optional = ["cofh-core"] // Optional Dependencies
    incompatible = ["fancy-menu"] // Incompatible dependencies
    embedded = ["my-api"] // Embedded dependencies
}

modrinthDepends {
    required = ["P7dR8mSH"] // Required Dependencies
    optional = ["nOiHJ1dx"] // Optional Dependencies
    incompatible = ["AANobbMI"] // Incompatible dependencies
    embedded = ["hvFnDODi", "9s6osm5g"] // Embedded dependencies
}
```
