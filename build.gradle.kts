plugins {
    kotlin("jvm") version "2.0.0"

    kotlin("plugin.serialization") version "2.0.0"

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

    // datetime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

    // reflect
    implementation(kotlin("reflect"))

    // logging
    implementation("io.github.oshai:kotlin-logging:7.0.0")
    val log4jVersion = "2.23.1"
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
    val kStoreVersion = "0.8.0"
    implementation("io.github.xxfast:kstore:$kStoreVersion")
    implementation("io.github.xxfast:kstore-file:$kStoreVersion")

    // ktor
    val ktorVersion = "2.3.12"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-resources:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // simbot
    val simbotVersion = "4.6.0"
    implementation("love.forte.simbot:simbot-core:$simbotVersion")
    compileOnly("love.forte.simbot.common:simbot-common-annotations:$simbotVersion")
    implementation("love.forte.simbot.component:simbot-component-onebot-v11-core:1.3.0")
    implementation("love.forte.simbot.component:simbot-component-kook-core:4.0.0")

    // kotter
    val kotterVersion = "1.1.2"
    implementation("com.varabyte.kotter:kotter:$kotterVersion")
    testImplementation("com.varabyte.kotterx:kotter-test-support:$kotterVersion")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}

application {
    mainClass = "NoMathExpectation.NMEBoot.MainKt"
}