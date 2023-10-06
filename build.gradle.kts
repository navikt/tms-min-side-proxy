import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm").version(Kotlin.version)
    kotlin("plugin.allopen").version(Kotlin.version)
    kotlin("plugin.serialization").version(Kotlin.version)
    id(Shadow.pluginId) version (Shadow.version)
    // Apply the application plugin to add support for building a CLI application.
    application
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

repositories {
    maven("https://jitpack.io")
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(Caffeine.caffeine)
    implementation(DittNAVCommonLib.utils)
    implementation(JacksonDatatype.datatypeJsr310)
    implementation(JacksonDatatype.moduleKotlin)
    implementation(Kotlinx.coroutines)
    implementation(KotlinLogging.logging)
    implementation(Ktor.Server.core)
    implementation(Ktor.Server.netty)
    implementation(Ktor.Server.auth)
    implementation(Ktor.Server.authJwt)
    implementation(Ktor.Server.defaultHeaders)
    implementation(Ktor.Server.cors)
    implementation(Ktor.Server.metricsMicrometer)
    implementation(Ktor.Server.statusPages)
    implementation(Ktor.Client.core)
    implementation(Ktor.Client.apache)
    implementation(Ktor.Client.contentNegotiation)
    implementation(Ktor.Serialization.jackson)
    implementation(Ktor.Server.contentNegotiation)
    implementation(TmsKtorTokenSupport.tokendingsExchange)
    implementation(TmsKtorTokenSupport.idportenSidecar)
    implementation(TmsKtorTokenSupport.azureExchange)
    implementation(Logstash.logbackEncoder)
    implementation(Micrometer.registryPrometheus)
    implementation(Prometheus.common)
    implementation(Prometheus.hotspot)
    implementation(Prometheus.logback)
    implementation(TmsCommonLib.commonLib)
    implementation("io.getunleash:unleash-client-java:8.2.1")

    testImplementation(Junit.api)
    testImplementation(Ktor.Test.clientMock)
    testImplementation(Ktor.Test.serverTestHost)
    testImplementation(Kotest.assertionsCore)
    testImplementation(KotlinTest.junit)

    testRuntimeOnly(Jjwt.impl)
    testRuntimeOnly(Junit.engine)
    testImplementation(Junit.params)
    testImplementation(Jjwt.api)
    testImplementation(Mockk.mockk)
    testImplementation(TmsKtorTokenSupport.idportenSidecarMock)
}

application {
    mainClassName = "no.nav.tms.min.side.proxy.ApplicationKt"
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

apply(plugin = Shadow.pluginId)
