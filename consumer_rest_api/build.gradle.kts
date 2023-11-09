

plugins {
    java
    application
    id("org.springframework.boot") version "3.1.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

application {
    mainClass.set("com.example.consumer.Application")
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    archiveFileName.set("consumer_rest_api-0.2.0.jar")
}