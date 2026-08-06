import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val signing = Properties().apply {
    val local = rootProject.file("keystore.properties")
    if (local.exists()) local.inputStream().use { load(it) }
}

fun secret(key: String): String? =
    signing.getProperty(key) ?: System.getenv(key)?.takeIf { it.isNotBlank() }

android {
    namespace = "net.spacealtctrl.discordrp"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.spacealtctrl.discordrp"
        minSdk = 26
        targetSdk = 36
        versionCode = (findProperty("versionCodeOverride") as String?)?.toInt()
            ?: libs.versions.app.version.code.get().toInt()
        versionName = libs.versions.app.version.name.get()

        buildConfigField("String", "DISCORD_API_URL", property("DISCORD_API_URL") as String)
        buildConfigField("String", "DISCORD_APP_ID", property("DISCORD_APP_ID") as String)

        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            val store = secret("KEYSTORE_FILE")?.let(::file)
            if (store?.exists() == true) {
                storeFile = store
                storePassword = secret("KEYSTORE_PASSWORD")
                keyAlias = secret("KEY_ALIAS")
                keyPassword = secret("KEY_PASSWORD") ?: secret("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.getByName("release").storeFile?.let {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    packaging.resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")

    lint {
        disable += "MissingTranslation"
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)
    implementation(libs.bundles.ktor)
    implementation(libs.coil.compose)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}
