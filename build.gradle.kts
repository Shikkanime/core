val ktorVersion = "2.3.9"
val ktorSwaggerUiVersion = "2.7.5"
val hibernateCoreVersion = "6.4.4.Final"
val ehcacheVersion = "3.10.8"
val glassfishJaxbVersion = "4.0.5"
val hibernateSearchVersion = "7.1.0.Final"
val tikaVersion = "3.0.0-BETA"
val postgresqlVersion = "42.7.3"
val reflectionsVersion = "0.10.2"
val guiceVersion = "7.0.0"
val liquibaseCoreVersion = "4.26.0"
val quartzVersion = "2.5.0-rc1"
val guavaVersion = "33.1.0-jre"
val playwrightVersion = "1.42.0"
val jsoupVersion = "1.17.2"
val gsonVersion = "2.10.1"
val openCvVersion = "4.9.0-0"
val bcprovVersion = "1.77"
val javaImageScalingVersion = "0.8.6"
val jdaVersion = "5.0.0-beta.21"
val twitter4jVersion = "4.0.7"
val twitter4jV2Version = "1.4.3"

val junitVersion = "5.10.2"
val h2Version = "2.2.224"

plugins {
    kotlin("jvm") version "2.0.0-Beta5"
    id("io.ktor.plugin") version "2.3.9"
    jacoco
    id("org.sonarqube") version "5.0.0.4638"
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
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-caching-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-compression-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")
    implementation("io.ktor:ktor-server-freemarker-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp-jvm:$ktorVersion")
    implementation("io.github.smiley4:ktor-swagger-ui:$ktorSwaggerUiVersion")
    implementation("org.hibernate.orm:hibernate-core:$hibernateCoreVersion")
    implementation("org.hibernate.orm:hibernate-jcache:$hibernateCoreVersion")
    implementation("org.ehcache:ehcache:$ehcacheVersion") {
        exclude(group = "org.glassfish.jaxb", module = "jaxb-runtime")
    }
    implementation("org.glassfish.jaxb:jaxb-runtime:$glassfishJaxbVersion")
    implementation("org.hibernate.search:hibernate-search-mapper-orm:$hibernateSearchVersion")
    implementation("org.hibernate.search:hibernate-search-backend-lucene:$hibernateSearchVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("org.reflections:reflections:$reflectionsVersion")
    implementation("com.google.inject:guice:$guiceVersion")
    implementation("org.liquibase:liquibase-core:$liquibaseCoreVersion")
    implementation("org.quartz-scheduler:quartz:$quartzVersion")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("com.microsoft.playwright:playwright:$playwrightVersion")
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("org.openpnp:opencv:$openCvVersion")
    implementation("org.bouncycastle:bcprov-jdk18on:$bcprovVersion")
    implementation("com.mortennobel:java-image-scaling:$javaImageScalingVersion")
    implementation("org.apache.tika:tika-core:$tikaVersion")
    implementation("org.apache.tika:tika-langdetect-optimaize:$tikaVersion")

    // Social networks
    implementation("net.dv8tion:JDA:$jdaVersion")
    implementation("org.twitter4j:twitter4j-core:$twitter4jVersion")
    implementation("io.github.takke:jp.takke.twitter4j-v2:$twitter4jV2Version")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("com.h2database:h2:$h2Version")
}

kotlin {
    jvmToolchain(21)
}

sonar {
    properties {
        property("sonar.projectKey", "core")
        property("sonar.projectName", "core")
        property("sonar.exclusions", "**/fr/shikkanime/socialnetworks/**,**/fr/shikkanime/jobs/FetchCalendarJob.kt")
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
