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
    implementation(DittNAVCommonLib.utils)
    implementation(JacksonDatatype.datatypeJsr310)
    implementation(Kotlinx.coroutines)
    implementation(KotlinLogging.logging)
    implementation(Kotlinx.htmlJvm)
    implementation(Ktor2.Server.core)
    implementation(Ktor2.Server.netty)
    implementation(Ktor2.Server.auth)
    implementation(Ktor2.Server.authJwt)
    implementation(Ktor2.Server.defaultHeaders)
    implementation(Ktor2.Server.cors)
    implementation(Ktor2.Server.metricsMicrometer)
    implementation(Ktor2.Server.statusPages)
    implementation(Ktor2.Client.core)
    implementation(Ktor2.Client.apache)
    implementation(Ktor2.Client.contentNegotiation)
    implementation(Ktor2.Serialization.kotlinX)
    implementation(Ktor2.Server.contentNegotiation)
    implementation(TmsKtorTokenSupport.tokendingsExchange)
    implementation(TmsKtorTokenSupport.idportenSidecar)
    implementation(TmsKtorTokenSupport.azureExchange)
    implementation(Logback.classic)
    implementation(Logstash.logbackEncoder)
    implementation(Micrometer.registryPrometheus)

    testImplementation(Junit.api)
    testImplementation(Ktor2.Test.clientMock)
    testImplementation(Ktor2.Test.serverTestHost)
    testImplementation(Kotest.assertionsCore)
    testImplementation(KotlinTest.junit)

    testRuntimeOnly(Jjwt.impl)
    testRuntimeOnly(Junit.engine)
    testImplementation(Junit.params)
    testImplementation(Jjwt.api)
    testImplementation(NAV.tokenValidatorKtor)
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
