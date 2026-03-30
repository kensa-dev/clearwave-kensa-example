import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.clearwave"
version = "1.0.0"

repositories {
    mavenLocal()        // for Kensa snapshots / local builds
    mavenCentral()
}

dependencies {
    implementation(libs.http4k.core)
    implementation(libs.http4k.jackson)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.jsr310)

    testImplementation(libs.kensa.junit)
    testImplementation(libs.kensa.kotest)
    testImplementation(libs.kensa.hamcrest)
    testImplementation(libs.hamcrest)
    testImplementation(libs.http4k.okhttp)
    testImplementation(platform(libs.kotest.bom))
    testImplementation(libs.kotest.assertions)
    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.launcher)
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll("-Xskip-prerelease-check")
    }
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "-Ddev.kensa.output.root=${layout.buildDirectory.get()}/kensa-output"
    )
}
