import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.5.6"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.spring") version "1.5.31"
}

group = "org.uqbar"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter:2.5.6")
    implementation("org.springframework.boot:spring-boot-starter-web:2.5.6")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.boot:spring-boot-starter-hateoas:2.5.6")
    implementation("org.springframework.boot:spring-boot-starter-data-neo4j:2.5.6")
    implementation("io.springfox:springfox-boot-starter:3.0.0")
    implementation("io.springfox:springfox-swagger-ui:3.0.0")
//    testImplementation("org.testcontainers:neo4j:1.16.2")
//    testImplementation("org.testcontainers:junit-jupiter:1.16.2")
    testImplementation("org.neo4j:neo4j-ogm-embedded-driver:3.2.26")
    testImplementation("org.neo4j.test:neo4j-harness:4.3.7")
    testImplementation("org.neo4j.driver:neo4j-java-driver-test-harness-spring-boot-autoconfigure:4.2.7.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.5.6")
}

configurations {
    all {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
