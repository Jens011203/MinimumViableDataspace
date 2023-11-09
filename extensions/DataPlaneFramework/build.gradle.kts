/*
 *  Copyright (c) Jens Feser

 */
plugins {
    `java-library`
    `java-test-fixtures`}

dependencies {

    implementation(libs.edc.core.controlplane.api.client)
    implementation(libs.edc.ext.dpf.util)
    implementation(libs.edc.core.connector)
    implementation(libs.edc.state.machine)
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.31.0")

}

