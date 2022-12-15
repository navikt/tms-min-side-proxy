import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm").version(Kotlin.version)
    kotlin("plugin.allopen").version(Kotlin.version)

    id(Shadow.pluginId) version (Shadow.version)
    // Apply the application plugin to add support for building a CLI application.
    application
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

repositories {
    // Use jcenter for resolving your dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
    maven("https://packages.confluent.io/maven")
    maven("https://jitpack.io")
    mavenLocal()
}

dependencies {
    implementation(DittNAV.Common.utils)
    implementation(Jackson.dataTypeJsr310)
    implementation(Kotlinx.coroutines)
    implementation(KotlinLogging.logging)
    implementation(Kotlinx.htmlJvm)
    implementation(Ktor2.Server.core)
    implementation(Ktor2.Server.netty)
    implementation(Ktor2.Server.auth)
    implementation(Ktor2.Server.authJwt)
    implementation(Ktor2.Server.defaultHeaders)
    implementation(Ktor2.Server.cors)
    implementation(Ktor2.Server.statusPages)
    implementation(Ktor2.Client.core)
    implementation(Ktor2.Client.apache)
    implementation(Ktor2.Client.contentNegotiation)
    implementation(Ktor2.kotlinX)
    implementation(Ktor2.Server.contentNegotiation)
    implementation(Ktor2.TmsTokenSupport.tokendingsExchange)
    implementation(Ktor2.TmsTokenSupport.idportenSidecar)
    implementation(Logback.classic)
    implementation(Logstash.logbackEncoder)

    testImplementation(Junit.api)
    testImplementation(Ktor2.Test.clientMock)
    testImplementation(Ktor2.Test.serverTestHost)
    testImplementation(Kotest.assertionsCore)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.7.10")


    testRuntimeOnly(Jjwt.impl)
    testRuntimeOnly(Junit.engine)
    testImplementation(Junit.params)
    testImplementation(Jjwt.api)
    testImplementation(NAV.tokenValidatorKtor)
    testImplementation(Mockk.mockk)
    testImplementation("com.github.navikt.tms-ktor-token-support:token-support-idporten-sidecar-mock:2.0.0")


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
