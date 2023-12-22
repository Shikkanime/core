val ktor_version: String by project
val kotlin_version: String by project

plugins {
    kotlin("jvm") version "1.9.21"
    id("io.ktor.plugin") version "2.3.7"
    jacoco
}

group = "fr.shikkanime"
version = "0.0.1"

application {
    mainClass.set("fr.shikkanime.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-sessions:$ktor_version")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-caching-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-compression-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-gson:$ktor_version")
    implementation("io.ktor:ktor-server-freemarker-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp:$ktor_version")
    implementation("io.github.smiley4:ktor-swagger-ui:2.7.2")
    implementation("org.hibernate.orm:hibernate-core:6.4.1.Final")
    implementation("org.hibernate.search:hibernate-search-mapper-orm:7.1.0.Alpha1")
    implementation("org.hibernate.search:hibernate-search-backend-lucene:7.1.0.Alpha1")
    implementation("org.postgresql:postgresql:42.7.0")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.google.inject:guice:7.0.0")
    implementation("org.liquibase:liquibase-core:4.25.1")
    implementation("org.quartz-scheduler:quartz:2.5.0-rc1")
    implementation("com.google.guava:guava:33.0.0-jre")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.16.0")
    implementation("com.microsoft.playwright:playwright:1.40.0")
    implementation("org.jsoup:jsoup:1.17.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.openpnp:opencv:4.8.1-0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    implementation("com.mortennobel:java-image-scaling:0.8.6")
    implementation("io.ktor:ktor-client-okhttp-jvm:2.3.7")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("io.ktor:ktor-client-mock:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("com.h2database:h2:2.2.224")
}

kotlin {
    jvmToolchain(21)
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}
