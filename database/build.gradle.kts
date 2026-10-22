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
