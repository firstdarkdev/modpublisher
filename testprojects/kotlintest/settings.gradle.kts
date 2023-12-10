pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://maven.firstdarkdev.xyz/releases")
        }
    }
}

includeBuild("../../")
rootProject.name = "kotlintest"