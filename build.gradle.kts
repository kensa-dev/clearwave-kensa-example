import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.clearwave"
version = "1.0.0"

repositories {
    mavenLocal()        // for Kensa snapshots / local builds
    mavenCentral()
    maven {
        name = "centralSnapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        mavenContent { snapshotsOnly() }
    }
}

configurations.all {
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlin.get()}")
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

sourceSets {
    create("uiTest") {
        kotlin.srcDir("src/uiTest/kotlin")
        resources.srcDir("src/uiTest/resources")
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
}

configurations {
    named("uiTestImplementation") { extendsFrom(configurations["testImplementation"]) }
    named("uiTestRuntimeOnly") { extendsFrom(configurations["testRuntimeOnly"]) }
}

dependencies {
    "uiTestImplementation"(libs.kensa.playwright.junit6)
    "uiTestImplementation"(libs.kensa.selenium.junit6)
    "uiTestImplementation"(libs.playwright.java)
    "uiTestImplementation"(libs.selenium.java)
}

val uiInstall by tasks.registering(Exec::class) {
    group = "build"
    description = "Install npm dependencies for the UI"

    workingDir = layout.projectDirectory.dir("ui").asFile

    val nodeModules = layout.projectDirectory.dir("ui/node_modules").asFile
    commandLine = if (nodeModules.exists()) listOf("npm", "install", "--silent") else listOf("npm", "ci", "--silent")

    inputs.file("ui/package.json")
    inputs.file("ui/package-lock.json")
    outputs.dir("ui/node_modules")

    onlyIf {
        val pkg = layout.projectDirectory.file("ui/package.json").asFile.lastModified()
        val lock = layout.projectDirectory.file("ui/package-lock.json").asFile.lastModified()
        val nm = nodeModules.lastModified()
        nm == 0L || lock > nm || pkg > nm
    }
}

val uiBuild by tasks.registering(Exec::class) {
    group = "build"
    description = "Build the Vite + shadcn UI under ui/"

    workingDir = layout.projectDirectory.dir("ui").asFile

    commandLine = listOf("npm", "run", "build", "--silent")

    dependsOn(uiInstall)

    inputs.dir("ui/src")
    inputs.file("ui/package.json")
    inputs.file("ui/package-lock.json")
    inputs.file("ui/vite.config.ts")
    inputs.file("ui/tsconfig.json")
    inputs.file("ui/tsconfig.app.json")
    outputs.dir("ui/dist")
}

val uiTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs UI tests (Playwright + Selenium) against the built UI"

    testClassesDirs = sourceSets["uiTest"].output.classesDirs
    classpath = sourceSets["uiTest"].runtimeClasspath

    useJUnitPlatform()

    dependsOn(uiBuild)

    jvmArgs(
        "-Ddev.kensa.output.root=${layout.buildDirectory.get()}/kensa-output-ui"
    )
}

val installPlaywrightBrowsers by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Install Playwright browsers (run once to set up local UI tests)"

    classpath = sourceSets["uiTest"].runtimeClasspath
    mainClass.set("com.microsoft.playwright.CLI")
    args = listOf("install", "--with-deps", "chromium")
}

tasks.test {
    useJUnitPlatform()
    dependsOn(uiBuild)
    jvmArgs(
        "-Ddev.kensa.output.root=${layout.buildDirectory.get()}/kensa-output"
    )
}
