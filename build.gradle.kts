val ktorVersion = "3.1.1"
val ktorSwaggerUiVersion = "4.0.0"
val schemaKenerator = "1.6.5"
val hibernateCoreVersion = "6.6.10.Final"
val ehcacheVersion = "3.10.8"
val hibernateSearchVersion = "7.2.2.Final"
val tikaVersion = "3.1.0"
val postgresqlVersion = "42.7.5"
val reflectionsVersion = "0.10.2"
val guiceVersion = "7.0.0"
val liquibaseCoreVersion = "4.31.1"
val quartzVersion = "2.5.0"
val playwrightVersion = "1.50.0"
val jsoupVersion = "1.19.1"
val gsonVersion = "2.12.1"
val openCvVersion = "4.9.0-0"
val bcprovVersion = "1.80"
val javaImageScalingVersion = "0.8.6"
val firebaseVersion = "9.4.3"
val simpleJavaMailVersion = "8.12.5"
val jacksonVersion = "2.18.3"

val jdaVersion = "5.3.0"
val twitter4jVersion = "4.0.7"
val twitter4jV2Version = "1.4.4"

val junitVersion = "5.12.0"
val h2Version = "2.3.232"
val mockkVersion = "1.13.17"

plugins {
    val kotlinVersion = "2.1.10"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    kotlin("kapt") version kotlinVersion

    id("io.ktor.plugin") version "3.1.1"
    id("org.sonarqube") version "6.0.1.5171"

    jacoco
}

group = "fr.shikkanime"
version = "0.19.0"

application {
    mainClass.set("fr.shikkanime.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")

    applicationDefaultJvmArgs = listOf(
        "-Dio.ktor.development=$isDevelopment",
        "--add-modules",
        "jdk.incubator.vector",
    )
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-caching-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-compression:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")
    implementation("io.ktor:ktor-server-freemarker:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")

    implementation("io.github.smiley4:ktor-swagger-ui:$ktorSwaggerUiVersion")
    implementation("io.github.smiley4:schema-kenerator-core:$schemaKenerator")
    implementation("io.github.smiley4:schema-kenerator-reflection:$schemaKenerator")
    implementation("io.github.smiley4:schema-kenerator-swagger:$schemaKenerator")
    implementation("org.hibernate.orm:hibernate-core:$hibernateCoreVersion")
    implementation("org.hibernate.orm:hibernate-jcache:$hibernateCoreVersion")
    implementation("org.ehcache:ehcache:$ehcacheVersion")
    implementation("org.hibernate.search:hibernate-search-mapper-orm:$hibernateSearchVersion")
    implementation("org.hibernate.search:hibernate-search-backend-lucene:$hibernateSearchVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("org.reflections:reflections:$reflectionsVersion")
    implementation("com.google.inject:guice:$guiceVersion")
    implementation("org.liquibase:liquibase-core:$liquibaseCoreVersion")
    implementation("org.quartz-scheduler:quartz:$quartzVersion")
    implementation("com.microsoft.playwright:playwright:$playwrightVersion")
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("org.openpnp:opencv:$openCvVersion")
    implementation("org.bouncycastle:bcprov-jdk18on:$bcprovVersion")
    implementation("com.mortennobel:java-image-scaling:$javaImageScalingVersion")
    implementation("org.apache.tika:tika-core:$tikaVersion")
    implementation("org.apache.tika:tika-langdetect-optimaize:$tikaVersion")
    implementation("com.google.firebase:firebase-admin:$firebaseVersion")
    implementation("org.simplejavamail:simple-java-mail:$simpleJavaMailVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("net.dv8tion:JDA:$jdaVersion")
    implementation("org.twitter4j:twitter4j-core:$twitter4jVersion")
    implementation("io.github.takke:jp.takke.twitter4j-v2:$twitter4jV2Version")

    kapt("org.hibernate.orm:hibernate-jpamodelgen:$hibernateCoreVersion")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("com.h2database:h2:$h2Version")
    testImplementation("io.mockk:mockk:$mockkVersion")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

sonar {
    properties {
        property("sonar.projectKey", "core")
        property("sonar.projectName", "core")
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy("jacocoTestReport")
}

tasks.jacocoTestReport {
    dependsOn("test")

    reports {
        xml.required = true
        html.required = false
    }
}

tasks.sonar {
    dependsOn("jacocoTestReport")
}
