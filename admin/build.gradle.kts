plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shikkanimeFrameworkKtor)
    alias(libs.plugins.shikkanimeFrameworkKoin)
    alias(libs.plugins.graalvmNative)
}

application {
    mainClass.set("fr.shikkanime.admin.ApplicationKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("shikkanime-admin")
            mainClass.set("fr.shikkanime.admin.ApplicationKt")
            sharedLibrary.set(false)
            jvmArgs.add("-Xmx${System.getenv("NATIVE_IMAGE_HEAP") ?: "3g"}")
            buildArgs.addAll(
                "--no-fallback",
                "-O0",
                "-march=compatibility",
                "-H:CCompilerOption=-march=x86-64",
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
    implementation(libs.shikkanimeFrameworkKtor)
    implementation(libs.shikkanimeFrameworkKoin)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testEcosystem)
    testImplementation(testFixtures(project(":database")))
}
