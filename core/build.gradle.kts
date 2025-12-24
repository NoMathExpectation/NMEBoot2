plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
    application
}

group = "NoMathExpectation.NMEBoot"
version = "2.0"

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0-RC")

    // datetime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

    // reflect
    implementation(kotlin("reflect"))

    // logging
    implementation("io.github.oshai:kotlin-logging:7.0.0")
    val log4jVersion = "2.25.3"
    runtimeOnly("org.apache.logging.log4j:log4j-api:$log4jVersion")
    runtimeOnly("org.apache.logging.log4j:log4j-core:$log4jVersion")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
    val slf4jVersion = "2.0.13"
    runtimeOnly("org.slf4j:slf4j-api:$slf4jVersion")

    // brigadier
    implementation("com.mojang:brigadier:1.0.18")

    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // KStore
    val kStoreVersion = "0.9.1"
    implementation("io.github.xxfast:kstore:$kStoreVersion")
    implementation("io.github.xxfast:kstore-file:$kStoreVersion")

    // ktor
    val ktorVersion = "2.3.13"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-resources:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-network:$ktorVersion")

    // simbot
    val simbotVersion = "4.14.0"
    implementation("love.forte.simbot:simbot-core:$simbotVersion")
    compileOnly("love.forte.simbot.common:simbot-common-annotations:$simbotVersion")
    implementation("love.forte.simbot.component:simbot-component-onebot-v11-core:1.9.0")
    implementation("love.forte.simbot.component:simbot-component-kook-core:4.3.0")

    // kotter
    val kotterVersion = "1.1.2"
    implementation("com.varabyte.kotter:kotter:$kotterVersion")
    testImplementation("com.varabyte.kotterx:kotter-test-support:$kotterVersion")

    // scripting
    implementation(project(":scripting-data"))

    // koin
    val koinVersion = "4.1.0"
    implementation(project.dependencies.platform("io.insert-koin:koin-bom:$koinVersion"))
    implementation("io.insert-koin:koin-core")
    val koinAnnotationVersion = "2.1.0"
    implementation(project.dependencies.platform("io.insert-koin:koin-annotations-bom:$koinAnnotationVersion"))
    implementation("io.insert-koin:koin-annotations")
    ksp("io.insert-koin:koin-ksp-compiler:$koinAnnotationVersion")

    // exposed
    val exposedVersion = "1.0.0-rc-2"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-r2dbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.postgresql:r2dbc-postgresql:1.1.1.RELEASE")
    implementation("com.zaxxer:HikariCP:6.3.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

application {
    mainClass = "NoMathExpectation.NMEBoot.MainKt"
}