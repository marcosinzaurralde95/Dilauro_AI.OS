plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.dilauro_aios"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.dilauroaios.kzxmpq"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val envFile = rootProject.file(".env")
        val envExampleFile = rootProject.file(".env.example")
        var geminiKey = ""
        if (envFile.exists()) {
            envFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("GEMINI_API_KEY=")) {
                    geminiKey = trimmed.substringAfter("GEMINI_API_KEY=").trim()
                }
            }
        }
        if (geminiKey.isEmpty() && envExampleFile.exists()) {
            envExampleFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("GEMINI_API_KEY=")) {
                    geminiKey = trimmed.substringAfter("GEMINI_API_KEY=").trim()
                }
            }
        }
        if (geminiKey.isEmpty()) {
            geminiKey = System.getenv("GEMINI_API_KEY") ?: ""
        }
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.okhttp)

    debugImplementation(libs.androidx.ui.tooling)
}
