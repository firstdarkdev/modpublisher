import com.hypherionmc.modpublisher.plugin.ModPublisherGradleExtension
import com.hypherionmc.modpublisher.properties.CurseEnvironment
import com.hypherionmc.modpublisher.properties.ModLoader

plugins {
    id("java")
    id("com.hypherionmc.modutils.modpublisher")
}

group = "me.hypherionmc.modutils"
version = "1.0.24"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

val dummyTask = tasks.register("createDummyJar", Jar::class) {
    // Configure the JAR task to have no files
    from(setOf<File>()) // Empty set of files
    archiveFileName.set("dummy.jar") // Set the desired name for the JAR file
}

publisher {
    apiKeys {
        curseforge(System.getenv("CURSE_TOKEN"))
        modrinth(System.getenv("MODRINTH_TOKEN"))
        github(System.getenv("GITHUB_TOKEN"))
    }

    debug.set(true)
    curseID.set("12345")
    modrinthID.set("sdjkg8867")
    githubRepo.set("OWNER/REPO")
    versionType.set("release")
    changelog.set("Coming Soon")
    projectVersion.set("1.3.0")
    displayName.set("MyAwesomeMod - Version")
    gameVersions.set(listOf("1.19.3", "1.19.4"))
    setGameVersions("1.19.3", "1.19.4")
    setLoaders(ModLoader.FORGE, ModLoader.FABRIC)
    setCurseEnvironment(CurseEnvironment.CLIENT)
    setJavaVersions(JavaVersion.VERSION_1_8, "Java 11", 17)
    disableMalwareScanner.set(false)
    disableEmptyJarCheck.set(true)
    artifact.set(tasks.jar)
    setPlatformArtifact("modrinth", dummyTask)
    useModrinthStaging.set(false)
    addAdditionalFile(tasks.jar)

    addAdditionalFile {
        artifact(tasks.jar)
        displayName("Test Name")
        changelog("Some Changelog")
    }

    github {
        tag("v${project.version}")
        createTag(true)
        createRelease(true)
        updateRelease(true)
    }

    curseDepends {
        required("fabric-api", "craterlib")
        optional("optional-mod", "another-mod")
        incompatible("breaks-with")
        embedded("rift")
    }

    modrinthDepends {
        required("fabric-api", "craterlib")
        optional("optional-mod")
        incompatible("breaks-with")
        embedded("rift")
    }
}