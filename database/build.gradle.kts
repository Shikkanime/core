plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.shikkanimeFrameworkKoin)
}

dependencies {
    implementation(project(":models"))
    implementation(libs.shikkanimeFrameworkCore)
    api(libs.shikkanimeFrameworkExposed)
    implementation(libs.shikkanimeFrameworkKoin)
    api(libs.shikkanimeFrameworkKoinExposed)
    implementation(libs.h2)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testEcosystem)
}
