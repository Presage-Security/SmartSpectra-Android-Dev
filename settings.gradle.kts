pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven( url ="https://jitpack.io")
        mavenLocal()
    }
}

rootProject.name = "PhysiologySdk"
include(":sdk")
include(":internal-demo")
