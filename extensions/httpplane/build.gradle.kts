/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *e
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(libs.edc.spi.dpf)
    api("org.eclipse.edc:data-plane-http-spi:0.3.1")
    api("org.eclipse.edc:http-spi:0.3.1")
    implementation("org.eclipse.edc:util:0.3.1")
    implementation("org.eclipse.edc:data-plane-util:0.3.1")
    runtimeOnly(project(":extensions:DataPlaneFramework"))
    implementation("org.apache.poi:poi:5.2.4")
    implementation("org.apache.poi:poi-ooxml:5.2.4")

    testImplementation("org.eclipse.edc:junit:0.3.1")
    implementation(project(":extensions:DataPlaneFramework"))
    //testImplementation("org.eclipse.edc:data-plane-core:0.3.1")

    //testImplementation(libs.restAssured)
    //testImplementation(libs.mockserver.netty)

    //testImplementation(testFixtures("org.eclipse.edc:data-plane-spi:0.2.1")))
}


