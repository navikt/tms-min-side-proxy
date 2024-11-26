import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm").version(Kotlin.version)

    id(Shadow.pluginId) version (Shadow.version)

    application
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    mavenLocal()
}

dependencies {
    implementation(Caffeine.caffeine)
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
    implementation(TmsKtorTokenSupport.tokenXValidation)
    implementation(Logstash.logbackEncoder)
    implementation(Prometheus.common)
    implementation(Prometheus.simpleClient)
    implementation(TmsCommonLib.metrics)
    implementation(TmsCommonLib.utils)
    implementation(TmsCommonLib.observability)
    implementation(Unleash.clientJava)

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
    testImplementation(TmsKtorTokenSupport.tokenXValidationMock)
    testImplementation(TmsCommonLib.testutils)
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
