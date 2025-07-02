rootProject.name = "testproject-spring-webflux"

includeBuild("../..")

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
//        maven("../build/project-local-repository")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}
