plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shikkanimeFrameworkKoin)
    application
}

application {
    mainClass.set("fr.shikkanime.jobs.ApplicationKt")
}

dependencies {
    implementation(project(":models"))
    implementation(project(":database"))
    implementation(libs.shikkanimeFrameworkCore)
    implementation(libs.shikkanimeFrameworkKoin)
    implementation(libs.shikkanimeFrameworkKtor)
    implementation(libs.quartz)
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testEcosystem)
    testImplementation(libs.shikkanimeFrameworkKtorTest)
}

// Benchmarks are excluded from `test` (and therefore from CI) because they allocate
// hundreds of megabytes and would slow down or OOM every standard run.
// They live in src/perfTest and run only via `./gradlew :jobs:perfTest`.
val perfTestSourceSet = sourceSets.create("perfTest") {
    compileClasspath += sourceSets["main"].output
    runtimeClasspath += sourceSets["main"].output
}

configurations["perfTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["perfTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

dependencies {
    "perfTestImplementation"(kotlin("test"))
}

tasks.register<Test>("perfTest") {
    description = "Runs the performance benchmarks (not part of `test`)."
    group = "verification"
    testClassesDirs = perfTestSourceSet.output.classesDirs
    classpath = perfTestSourceSet.runtimeClasspath
    // Benchmarks build multi-hundred-megabyte payloads, so the default heap is not enough.
    maxHeapSize = "4g"
    shouldRunAfter(tasks.test)
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}

// `internal` declarations of the main source set (AdnVideo, AdnShow, …) are visible to
// the `test` source set because Gradle makes test an associated/friend compilation.
// perfTest is a separate compilation and does not get that for free: without the
// friend path, every reference to an internal type fails to compile.
tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compilePerfTestKotlin") {
    // The friend path must point at the main compilation output directory.
    val mainOutput = project.layout.buildDirectory.dir("classes/kotlin/main")
    compilerOptions {
        freeCompilerArgs.addAll(listOf("-Xfriend-paths=${mainOutput.get().asFile}"))
    }
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "1g"
    testLogging {
        events("passed", "failed", "skipped")
    }
}
