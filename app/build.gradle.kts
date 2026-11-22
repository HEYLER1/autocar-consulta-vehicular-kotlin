import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val placApiKey = (localProperties.getProperty("PLACAPI_KEY") ?: "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.example.app_autocar"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.app_autocar"
        minSdk = 34
        targetSdk = 37
        versionCode = 42
        versionName = "1.42"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // ONNX Runtime solo se empaqueta para ABIs de 64 bits y ARMv7; el resto usa el
            // ingreso manual de captchas.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "PLACAPI_KEY", "\"$placApiKey\"")
        }
        release {
            buildConfigField("String", "PLACAPI_KEY", "\"\"")
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Release snapshots are immutable: bump versionName before saving the next update.
val autocarVersion = android.defaultConfig.versionName!!
val versionedApk = rootProject.layout.projectDirectory.file("dist/AUTOCAR-$autocarVersion.apk")
tasks.register<Copy>("saveVersionedApk") {
    val snapshotName = "AUTOCAR-$autocarVersion.apk"
    val snapshotFile = versionedApk.asFile
    dependsOn("assembleDebug")
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(rootProject.layout.projectDirectory.dir("dist"))
    rename { snapshotName }
    onlyIf("Preserve previously saved APK versions") { !snapshotFile.exists() }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation(libs.onnxruntime.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
