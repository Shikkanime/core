plugins {
    id("buildsrc.convention.kotlin-jvm")
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
    implementation(libs.quartz)
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testEcosystem)
}
