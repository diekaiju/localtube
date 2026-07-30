pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
include("localServerApp")

include("extractor")
project(":extractor").projectDir = file("NewPipeExtractor/extractor")

include("timeago-generator")
project(":timeago-generator").projectDir = file("NewPipeExtractor/timeago-generator")

rootProject.name = "NewPipeExtractor"
