import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

// Release-Signing: Zugangsdaten liegen in der nicht versionierten keystore.properties
// (siehe .gitignore). Fehlt die Datei (z. B. auf einem anderen Rechner, in der CI oder bei
// F-Droid), wird der Release-Build unsigniert gebaut statt abzubrechen.
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}
val storeFilePath = keystoreProperties.getProperty("storeFile", "")

android {
    namespace = "io.github.glaside.zahlenschatzjagd"
    compileSdk {
        version =
            release(36) {
                minorApiLevel = 1
            }
    }

    defaultConfig {
        applicationId = "io.github.glaside.zahlenschatzjagd"
        minSdk = 26
        targetSdk = 36
        // Bei jedem verteilten Release beide erhöhen und einen Git-Tag setzen,
        // sonst ist später nicht feststellbar, was in einer APK steckt (siehe docs/agents/release.md).
        versionCode = 19
        versionName = "2.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (storeFilePath.isNotEmpty()) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = keystoreProperties.getProperty("storePassword", "")
                keyAlias = keystoreProperties.getProperty("keyAlias", "")
                keyPassword = keystoreProperties.getProperty("keyPassword", "")
            }
        }
    }

    buildTypes {
        release {
            // R8 kürzt den Code. Der Build muss dabei bitgleich nachbaubar bleiben, sonst baut
            // F-Droid die Fassung nicht (#6, ADR-0033).
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (storeFilePath.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Ohne diesen Schalter legt AGP eine verschlüsselte Abhängigkeitsliste in den Signaturblock,
    // die nur Google lesen kann. F-Droid beanstandet sie (#3).
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    buildFeatures {
        compose = true
        // Für BuildConfig.VERSION_NAME: wird in die Sicherungsdatei geschrieben, damit später
        // nachvollziehbar ist, aus welcher App-Version eine Sicherung stammt.
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Stubt den Android-Dateidialog: Das Einspielen einer Sicherung ist sonst nicht ohne
    // Handgriff prüfbar, weil der Dialog dem System gehört (siehe ProfileOnboardingInstrumentedTest).
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // --- ROOM DATENBANK ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    detektPlugins(libs.detekt.formatting)
}

detekt {
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
}
