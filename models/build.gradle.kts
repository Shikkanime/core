plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(libs.kotlinxDateTime)
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testEcosystem)
}
