val ktorVersion = "3.3.3"
val hibernateCoreVersion = "7.1.11.Final"
val ehcacheVersion = "3.11.1"
val hibernateSearchVersion = "8.1.2.Final"
val postgresqlVersion = "42.7.8"
val reflectionsVersion = "0.10.2"
val guiceVersion = "7.0.0"
val liquibaseCoreVersion = "5.0.1"
val quartzVersion = "2.5.2"
val playwrightVersion = "1.56.0"
val jsoupVersion = "1.21.2"
val gsonVersion = "2.13.2"
val bcprovVersion = "1.83"
val javaImageScalingVersion = "0.8.6"
val firebaseVersion = "9.7.0"
val simpleJavaMailVersion = "8.12.6"
val jacksonVersion = "2.20.1"
val valkeyVersion = "2.0.1"
val apachePoiVersion = "5.5.1"

val jdaVersion = "6.1.2"

val junitVersion = "6.0.1"
val h2Version = "2.4.240"
val mockkVersion = "1.14.6"

plugins {
    val kotlinVersion = "2.2.21"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    kotlin("kapt") version kotlinVersion

    id("io.ktor.plugin") version "3.3.3"
    id("org.sonarqube") version "7.1.0.6387"
    id("com.google.osdetector") version "1.7.3"

    jacoco
}

group = "fr.shikkanime"
version = "0.23.26"

application {
    mainClass.set("fr.shikkanime.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")

    applicationDefaultJvmArgs = listOf(
        "-Dio.ktor.development=$isDevelopment",
        "--add-modules",
        "jdk.incubator.vector",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=./dumps/java_pid.hprof"
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
    implementation("org.bouncycastle:bcprov-jdk18on:$bcprovVersion")
    implementation("com.mortennobel:java-image-scaling:$javaImageScalingVersion")
    implementation("com.google.firebase:firebase-admin:$firebaseVersion")
    implementation("org.simplejavamail:simple-java-mail:$simpleJavaMailVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation(group = "io.valkey", name = "valkey-glide", version = valkeyVersion, classifier = osdetector.classifier)
    implementation("org.apache.poi:poi-ooxml:$apachePoiVersion")
    implementation("org.apache.poi:poi:$apachePoiVersion")

    implementation("net.dv8tion:JDA:$jdaVersion")

    kapt("org.hibernate.orm:hibernate-jpamodelgen:$hibernateCoreVersion")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("com.h2database:h2:$h2Version")
    testImplementation("io.mockk:mockk:$mockkVersion")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sonar {
    properties {
        property("sonar.projectKey", "shikkanime-core")
        property("sonar.projectName", "Shikkanime Core")
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
