plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shikkanimeFrameworkKoin)
    application
}

application {
    mainClass.set("fr.shikkanime.api.ApiApplicationKt")
}

dependencies {
    implementation(project(":models"))
    implementation(project(":database"))
    implementation(libs.shikkanimeFrameworkCore)
    implementation(libs.shikkanimeFrameworkKoin)
    implementation(libs.shikkanimeFrameworkKtor)
    implementation(libs.shikkanimeFrameworkGraphql)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testEcosystem)
    testImplementation(libs.shikkanimeFrameworkKtorTest)
    testImplementation(testFixtures(project(":database")))
}

// End-to-end runs against a real embedded server live in src/integrationTest and run via
// `./gradlew :api:integrationTest`: they boot the application and send real HTTP requests, so a
// wiring problem (a serializer missing from content negotiation, a route that never registers)
// fails here instead of on a manual run.
val integrationTestSourceSet = sourceSets.create("integrationTest") {
    compileClasspath += sourceSets["main"].output
    runtimeClasspath += sourceSets["main"].output
}

configurations["integrationTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

dependencies {
    "integrationTestImplementation"(kotlin("test"))
}

tasks.register<Test>("integrationTest") {
    description = "Runs the end-to-end integration tests (not part of `test`)."
    group = "verification"
    useJUnitPlatform()
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    shouldRunAfter(tasks.test)
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}

kotlin.target.compilations.named("integrationTest") {
    associateWith(kotlin.target.compilations.getByName("main"))
}
