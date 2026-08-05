plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shikkanimeFrameworkKtor)
    alias(libs.plugins.shikkanimeFrameworkKoin)
}

application {
    mainClass.set("fr.shikkanime.admin.ApplicationKt")
}

dependencies {
    implementation(project(":models"))
    implementation(project(":database"))
    implementation(libs.shikkanimeFrameworkCore)
    implementation(libs.shikkanimeFrameworkKtor)
    implementation(libs.shikkanimeFrameworkKoin)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testEcosystem)
}
