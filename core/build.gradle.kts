import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// Pure Kotlin/JVM. No Android, no AndroidX — this module must stay resolvable
// from Maven Central alone so it can be built and tested without the Android
// SDK. See docs/ARCHITECTURE.md.

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        // Explicit jvmTarget instead of a toolchain block: the container has
        // only JDK 21, and toolchain auto-provisioning is another network
        // dependency that may be blocked. See docs/DECISIONS.md D-012.
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
