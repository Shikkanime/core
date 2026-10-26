plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.shikkanimeFrameworkKoin)
    `java-test-fixtures`
    application
}

// Ships as its own distribution: every application image provisions the schema before it starts.
application {
    mainClass.set("fr.shikkanime.database.ApplicationKt")
}

dependencies {
    implementation(project(":models"))
    implementation(libs.shikkanimeFrameworkCore)
    api(libs.shikkanimeFrameworkExposed)
    implementation(libs.shikkanimeFrameworkKoin)
    api(libs.shikkanimeFrameworkKoinExposed)
    implementation(libs.h2)

    testFixturesApi(libs.bundles.testEcosystem)
    testFixturesApi(project(":models"))
    testFixturesApi(libs.shikkanimeFrameworkCore)
    testFixturesApi(libs.shikkanimeFrameworkExposed)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testEcosystem)
}

// The image codec loads the system libwebp: forward an explicit path to the test JVM when the build
// environment provides one, so a non-system install is still exercised.
tasks.withType<Test>().configureEach {
    System.getenv("WEBP_LIBRARY")?.let { systemProperty("webp.library", it) }
}
