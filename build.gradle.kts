import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(managed.plugins.kotlin)
    alias(managed.plugins.tms.jarBundling)
    application
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

jarBundling {
    applicationName = "tms-min-side-proxy"
    outputDirectory = "build/libs"
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    mavenLocal()
}

dependencies {
    implementation(libs.caffeine)
    implementation(libs.ktor.server.auth.jwt) // vuln i denne CVE-2026-85473, men ingen fix siden 3.6.0 er nyest
    implementation(managed.jackson.datatypeJsr310)
    implementation(managed.jackson.moduleKotlin)
    implementation(managed.kotlinx.coroutines)
    implementation(managed.kotlinLogging)
    implementation(managed.bundles.ktorServer)
    implementation(managed.bundles.ktorClient)
    implementation(managed.ktor.serialization.jackson)
    implementation(managed.ktor.server.cors)
    implementation(managed.tms.ktorTokenSupport.userToken.loginRoutes)
    implementation(managed.tms.ktorTokenSupport.userToken.exchange)
    implementation(managed.tms.ktorTokenSupport.userToken.verification)
    implementation(managed.tms.ktorTokenSupport.entraId.fetcher)
    implementation(managed.ktor.server.defaultHeaders)
    implementation(managed.logback.encoder)
    implementation(managed.micrometer.registry.prometheus)
    implementation(managed.prometheus.metric.core)
    implementation(managed.tms.commonLib.utils)
    implementation(managed.tms.commonLib.metrics)
    implementation(managed.tms.commonLib.observability)
    implementation(managed.tms.commonLib.teamLogger)

    testImplementation(libs.kotlin.junit)
    testImplementation(managed.junit.launcher)
    testImplementation(managed.junit.api)
    testImplementation(managed.junit.engine)
    testImplementation(managed.ktor.test.clientMock)
    testImplementation(managed.ktor.test.server)
    testImplementation(managed.kotest.assertion)
    testImplementation(managed.jjwt.api)
    testImplementation(managed.mockk)
    testImplementation(managed.tms.ktorTokenSupport.userToken.verificationMock)

    testRuntimeOnly(managed.jjwt.impl)
}

application {
    mainClass.set("no.nav.tms.min.side.proxy.ApplicationKt")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events("passed", "skipped", "failed")
        }
    }
}
