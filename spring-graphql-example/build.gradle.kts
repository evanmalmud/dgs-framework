/*
 * Copyright 2021 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-graphql:3.1.0")

    //implementation(project(":graphql-dgs-spring-graphql-starter"))

    // Enabling Spring Boot Actuators. We are not expressing any opinion on which Metric System should be used
    // please review the Spring Boot Docs in the link below for additional information.
    // https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-features.html#production-ready-metrics-export-simple
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Adding the DGS Micrometer integration that provides timers for gql.*
    // For additional information go to the link below:
    // https://netflix.github.io/dgs/advanced/instrumentation/

    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("io.projectreactor:reactor-test")
}
