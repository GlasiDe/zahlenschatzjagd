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

// Versionen, deren Änderungshinweis die App nach dem Update einmal beim Start zeigt (#8), als
// versionCode to versionName. Der Text steht allein in
// fastlane/metadata/android/de-DE/changelogs/<versionCode>.txt – derselbe, den F-Droid zeigt.
// Einträge bleiben stehen, auch wenn längst neuere Versionen da sind: Wer eine Version
// überspringt, bekommt ihren Hinweis trotzdem. Wann eine Version hierher gehört, steht in
// docs/agents/release.md.
val aenderungshinweiseInDerApp: Map<Int, String> = mapOf(20 to "2.1.0")

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
        // Dazu entscheiden, ob die neue Version in aenderungshinweiseInDerApp (oben) gehört.
        versionCode = 20
        versionName = "2.1.0"

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

/**
 * Schreibt die markierten Änderungshinweise als `AENDERUNGSHINWEISE` in eine Kotlin-Datei (#8).
 *
 * F-Droid baut aus dem Tag nach und liefert nur aus, wenn das Ergebnis bitgleich zu unserer APK
 * ist (ADR-0033). Die Ausgabe darf deshalb nur vom Inhalt abhängen: feste Reihenfolge, kein
 * Zeitstempel, und Zeilenenden, die nicht davon abhängen, ob Git unter Windows `\r\n` ausgecheckt
 * hat.
 */
abstract class AenderungshinweiseErzeugen : DefaultTask() {
    @get:Input
    abstract val versionen: MapProperty<Int, String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val changelogs: DirectoryProperty

    @get:OutputDirectory
    abstract val ausgabe: DirectoryProperty

    @TaskAction
    fun erzeuge() {
        val eintraege = versionen.get().toSortedMap().map { (code, name) ->
            val datei = changelogs.file("$code.txt").get().asFile
            if (!datei.isFile) {
                throw GradleException(
                    "Version $code ist in aenderungshinweiseInDerApp markiert, aber " +
                        "${datei.path} fehlt.",
                )
            }
            val text = datei.readText(Charsets.UTF_8).replace("\r", "").trim()
            "    Aenderungshinweis(versionCode = $code, versionName = ${literal(name)}, " +
                "text = ${literal(text)}),"
        }
        val ziel = ausgabe.get().asFile
        ziel.deleteRecursively()
        val datei = File(ziel, "io/github/glaside/zahlenschatzjagd/data/Aenderungshinweise.kt")
        datei.parentFile.mkdirs()
        datei.writeText(
            buildString {
                append("// Erzeugt von app/build.gradle.kts aus fastlane/metadata/android/")
                append("de-DE/changelogs – nicht von Hand ändern.\n")
                append("package io.github.glaside.zahlenschatzjagd.data\n\n")
                append("internal val AENDERUNGSHINWEISE: List<Aenderungshinweis> = listOf(\n")
                eintraege.forEach { append(it).append('\n') }
                append(")\n")
            },
            Charsets.UTF_8,
        )
    }

    private fun literal(wert: String): String =
        "\"" + wert.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")
            .replace("\n", "\\n") + "\""
}

androidComponents {
    onVariants { variant ->
        val erzeuge = tasks.register<AenderungshinweiseErzeugen>(
            "erzeugeAenderungshinweise" + variant.name.replaceFirstChar { it.uppercaseChar() },
        ) {
            versionen.set(aenderungshinweiseInDerApp)
            changelogs.set(
                rootProject.layout.projectDirectory.dir("fastlane/metadata/android/de-DE/changelogs"),
            )
        }
        variant.sources.kotlin?.addGeneratedSourceDirectory(erzeuge, AenderungshinweiseErzeugen::ausgabe)
    }
}
