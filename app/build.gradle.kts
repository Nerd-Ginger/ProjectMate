plugins {
    // No kotlin-android plugin: AGP 9 has built-in Kotlin support and applying
    // org.jetbrains.kotlin.android alongside it is a hard error. The Compose,
    // serialization and KSP plugins are still applied separately.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.nerdginger.projectmate"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nerdginger.projectmate"
        minSdk = 26
        targetSdk = 37

        // CI artifacts must be installable over each other. Deriving the code
        // from the run number makes each build a genuine upgrade.
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // A committed, non-secret debug keystore. Without a stable key, every
        // CI build is signed differently and installing an update fails with
        // INSTALL_FAILED_UPDATE_INCOMPATIBLE — which means uninstalling and
        // losing all app data on every update. See docs/DECISIONS.md D-013.
        //
        // Debug keys are non-secret by design; release signing is a different
        // matter and must never be committed.
        getByName("debug") {
            val debugKeystore = rootProject.file("debug.keystore")
            if (debugKeystore.exists()) {
                storeFile = debugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        // No jvmToolchain block — see docs/DECISIONS.md D-012.
        // With AGP's built-in Kotlin, jvmTarget defaults to
        // targetCompatibility, so there is nothing further to configure.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // packaging {}, testOptions {} and lint {} are deliberately absent until the
    // first green build. Each is a convenience rather than a requirement, and
    // each is a candidate for the AGP 9 DSL change that is currently breaking
    // this script. They go back in one at a time once the build compiles.
}

// Room 3 exports its schema JSON here; the directory is committed so that
// migrations can be tested against real historical schemas.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.adaptive.navigation.suite)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.room.runtime)
    implementation(libs.sqlite.bundled)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Room 3 runs on the JVM via the bundled SQLite driver, so DAO and
    // migration tests are plain unit tests — no emulator required.
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.room.testing)
    testImplementation(libs.sqlite.bundled)
    testImplementation(libs.turbine)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
