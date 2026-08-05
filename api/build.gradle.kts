plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
}

application {
    mainClass.set("fr.shikkanime.api.ApiApplicationKt")
}

dependencies {
    implementation(project(":models"))
    implementation(project(":database"))
    implementation(libs.shikkanimeFrameworkCore)
    implementation(libs.shikkanimeFrameworkKtor)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testEcosystem)
}
