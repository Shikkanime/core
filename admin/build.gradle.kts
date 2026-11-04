import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.process.ExecOperations

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

/**
 * Flutter project of the administration interface, served by this application.
 *
 * The web build is packaged under the `webapp` resource package, so the interface and the API it
 * reads share one origin: starting this module serves both.
 */
val webAppProject = layout.projectDirectory.dir("webapp")

/**
 * Output of the Flutter build, and the resource package `WebApp.kt` mounts.
 */
val webApp = layout.buildDirectory.dir("webapp")

/**
 * Compiles the Flutter web administration into its resource package.
 *
 * The Flutter toolchain is required only when the compiled app is absent: the image build ships the
 * artifact compiled by the `Admin web app` CI job, and a developer compiles it here.
 */
abstract class BuildWebApp : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val flutterProject: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    /** Flutter executable, or the SDK directory it lives in (`FLUTTER_BIN`). */
    @get:Input
    abstract val flutter: Property<String>

    /** Compiled interface handed over by an image build, empty when this build compiles it. */
    @get:Input
    @get:Optional
    abstract val prebuilt: Property<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    /**
     * Resolves the command to run.
     *
     * `FLUTTER_BIN` accepts the executable, the SDK root or its `bin` directory: a developer exports
     * whichever path the shell already holds, and the build reads it the same way.
     */
    private fun executable(): String {
        val configured = flutter.get()
        val candidate = File(configured)

        return when {
            candidate.isFile -> candidate.absolutePath
            candidate.resolve("bin/flutter").isFile -> candidate.resolve("bin/flutter").absolutePath
            candidate.resolve("flutter").isFile -> candidate.resolve("flutter").absolutePath
            else -> configured
        }
    }

    /**
     * Runs `flutter --version`, or returns `null` with the reason logged.
     *
     * A silent `false` made the failure impossible to diagnose: the reason is logged here and the
     * caller's message names the path it tried.
     */
    private fun flutterVersion(): String? {
        val output = ByteArrayOutputStream()
        val error = ByteArrayOutputStream()
        val command = executable()

        val exit = runCatching {
            execOperations.exec {
                commandLine(command, "--version")
                standardOutput = output
                errorOutput = error
                isIgnoreExitValue = true
            }.exitValue
        }.getOrElse { failure ->
            logger.info("Flutter cannot be run from '$command': ${failure.message}")
            return null
        }

        if (exit != 0) {
            logger.info("Flutter exited with $exit from '$command': ${error.toString().trim().take(500)}")
            return null
        }

        return output.toString().trim().lineSequence().firstOrNull()
    }

    @TaskAction
    fun build() {
        val target = output.get().asFile
        val project = flutterProject.get().asFile
        val prebuiltDirectory = prebuilt.orNull?.takeIf { it.isNotBlank() }?.let(::File)

        if (prebuiltDirectory != null && prebuiltDirectory.resolve("index.html").isFile) {
            logger.lifecycle("Packaging the compiled Flutter web app from $prebuiltDirectory.")
            prebuiltDirectory.copyRecursively(target, overwrite = true)
            return
        }

        val version = flutterVersion()

        if (version == null) {
            check(target.resolve("index.html").isFile) {
                "Flutter cannot be run from '${executable()}': put it in PATH, point FLUTTER_BIN at " +
                    "the SDK directory or its bin/flutter, or provide a compiled web app in $target."
            }
            logger.lifecycle("Using the compiled Flutter web app from $target.")
            return
        }

        logger.lifecycle("Building the web app with $version")

        execOperations.exec {
            commandLine(executable(), "build", "web", "--release", "--base-href", "/")
            workingDir = project
        }

        project.resolve("build/web").copyRecursively(target, overwrite = true)
    }
}

val buildWebApp = tasks.register<BuildWebApp>("buildWebApp") {
    group = "build"
    description = "Builds the Flutter web administration served by the admin application."

    flutterProject.set(webAppProject)
    output.set(webApp)
    flutter.set(providers.environmentVariable("FLUTTER_BIN").orElse("flutter"))
    prebuilt.set(providers.environmentVariable("WEB_APP_DIST").orElse(""))
}

tasks.named<ProcessResources>("processResources") {
    // The compiled interface is a resource of the application, not a source directory of the repo.
    // The task itself is bound to the tasks that serve or distribute the module, never to `test`:
    // a unit test does not need the interface, and compiling it would cost a Flutter toolchain.
    from(webApp) {
        into("webapp")
    }
    mustRunAfter(buildWebApp)
}

tasks.named<JavaExec>("run") {
    dependsOn(buildWebApp)
}

tasks.matching { it.name in setOf("installDist", "distZip", "distTar") }.configureEach {
    dependsOn(buildWebApp)
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
