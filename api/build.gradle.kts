plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shikkanimeFrameworkKoin)
    alias(libs.plugins.graalvmNative)
    application
}

application {
    mainClass.set("fr.shikkanime.api.ApplicationKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("shikkanime-api")
            mainClass.set("fr.shikkanime.api.ApplicationKt")
            sharedLibrary.set(false)
            jvmArgs.add("-Xmx${System.getenv("NATIVE_IMAGE_HEAP") ?: "3g"}")
            buildArgs.addAll(
                "--no-fallback",
                "-O1",
                "-H:+ReportExceptionStackTraces",
                "--enable-url-protocols=http,https"
            )
        }
    }
}

dependencies {
    implementation(project(":models"))
    implementation(project(":database"))
    implementation(libs.shikkanimeFrameworkCore)
    implementation(libs.shikkanimeFrameworkKoin)
    implementation(libs.shikkanimeFrameworkKtor)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testEcosystem)
    testImplementation(libs.shikkanimeFrameworkKtorTest)
    testImplementation(testFixtures(project(":database")))
}

