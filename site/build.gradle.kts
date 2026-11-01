plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
    alias(libs.plugins.graalvmNative)
}

application {
    mainClass.set("fr.shikkanime.site.SiteApplicationKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("shikkanime-site")
            mainClass.set("fr.shikkanime.site.SiteApplicationKt")
            sharedLibrary.set(false)
            jvmArgs.add("-Xmx${System.getenv("NATIVE_IMAGE_HEAP") ?: "3g"}")
            buildArgs.addAll(
                "--no-fallback",
                "-O0",
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

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testEcosystem)
}
