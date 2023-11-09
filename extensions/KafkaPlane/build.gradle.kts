/*
 *  Copyright (c) Jens Feser

 */
plugins {
    `java-library`
    `java-test-fixtures`}

dependencies {
    implementation(project(":extensions:DataPlaneFramework"))
    implementation(libs.edc.core.controlplane.api.client)
    implementation(libs.edc.ext.dpf.util)
    implementation(libs.edc.core.connector)
    implementation(libs.kafka.client)
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.0-rc1")
    testImplementation(libs.edc.core.junit)
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.31.0")

}