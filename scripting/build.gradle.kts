plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("io.ktor.plugin") version "3.1.0"
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    application
}

group = "NoMathExpectation.NMEBoot"
version = "2.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0-RC")

    // logging
    implementation("io.github.oshai:kotlin-logging:7.0.0")
    val log4jVersion = "2.23.1"
    runtimeOnly("org.apache.logging.log4j:log4j-api:$log4jVersion")
    runtimeOnly("org.apache.logging.log4j:log4j-core:$log4jVersion")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
    val slf4jVersion = "2.0.13"
    runtimeOnly("org.slf4j:slf4j-api:$slf4jVersion")

    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // KStore
    val kStoreVersion = "0.9.1"
    implementation("io.github.xxfast:kstore:$kStoreVersion")
    implementation("io.github.xxfast:kstore-file:$kStoreVersion")

    // ktor
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-cio")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-call-logging")

    // scripting
    implementation("org.jetbrains.kotlin:kotlin-scripting-common")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven")
    implementation(project(":scripting-data"))

    // koin
    val koinVersion = "4.0.2"
    implementation(project.dependencies.platform("io.insert-koin:koin-bom:$koinVersion"))
    implementation("io.insert-koin:koin-core")
    val koinAnnotationVersion = "1.4.0"
    implementation(project.dependencies.platform("io.insert-koin:koin-annotations-bom:$koinAnnotationVersion"))
    api("io.insert-koin:koin-annotations")
    ksp("io.insert-koin:koin-ksp-compiler")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass = "NoMathExpectation.NMEBoot.scripting.MainKt"
}