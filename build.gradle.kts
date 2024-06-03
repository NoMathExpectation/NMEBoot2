plugins {
    kotlin("jvm") version "2.0.0"
}

group = "NoMathExpectation.NMEBoot"
version = "2.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}