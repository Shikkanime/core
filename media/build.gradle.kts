plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testEcosystem)
}

// The codec loads the system libwebp: forward an explicit path to the test JVM when the build
// environment provides one, so a non-system install is still exercised.
tasks.withType<Test>().configureEach {
    System.getenv("WEBP_LIBRARY")?.let { systemProperty("webp.library", it) }
}
