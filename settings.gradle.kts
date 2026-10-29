rootProject.name = "tms-min-side-proxy"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("managed") {
            from("no.nav.tms:tms-dependency-admin:20261027.8.1")
        }
    }
    pluginManagement {
        repositories {
            gradlePluginPortal()
            maven(url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release"))
        }
    }
}