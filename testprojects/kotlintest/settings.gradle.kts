pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://maven.firstdarkdev.xyz/releases")
        }
        maven("https://jitpack.io")
    }
}

includeBuild("../../")
rootProject.name = "kotlintest"