package buildsrc.convention

import org.gradle.api.tasks.testing.logging.TestLogEvent

group = "fr.shikkanime"
version = providers.gradleProperty("version").orNull ?: "1.0.0-SNAPSHOT"

plugins {
    kotlin("jvm")
    `java-library`
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
    }
}
