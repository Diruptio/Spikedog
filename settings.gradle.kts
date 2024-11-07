pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.diruptio.de/repository/maven-public-releases/")
    }
}

rootProject.name = "Spikedog"

include("example-module")
include("info-module")
include("reload-module")
include("spikedev")
