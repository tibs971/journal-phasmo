import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Le niveau d'API et les build-tools peuvent être imposés en ligne de commande, pour
// construire avec ce qui est DÉJÀ installé plutôt que d'obliger Gradle à télécharger.
// C'est ce que fait publier-android.cmd : il regarde le SDK et passe ce qu'il y trouve.
//   gradlew assembleDebug -PsdkCible=36 -PoutilsCible=36.0.0
val sdkCible = (project.findProperty("sdkCible") as String?)?.toIntOrNull() ?: 35
val outilsCible = project.findProperty("outilsCible") as String?

android {
    namespace = "io.github.tibs971.journalphasmo"
    compileSdk = sdkCible
    if (outilsCible != null) buildToolsVersion = outilsCible

    defaultConfig {
        // ATTENTION : cet identifiant ne peut plus changer une fois l'appli publiée
        // sur Google Play. Il est construit sur le compte GitHub tibs971.
        applicationId = "io.github.tibs971.journalphasmo"
        minSdk = 24
        targetSdk = sdkCible
        versionCode = 1
        versionName = "1.0.0"
    }

    // La clé de signature, SI elle existe. Le fichier keystore.properties n'est jamais
    // versionné (voir .gitignore) : il contient les mots de passe. Sans lui, seule la
    // version de débogage se construit — ce qui suffit pour essayer sur son téléphone.
    val fichierCle = rootProject.file("keystore.properties")
    val cle = Properties().apply {
        if (fichierCle.exists()) fichierCle.inputStream().use { load(it) }
    }

    signingConfigs {
        if (fichierCle.exists()) {
            create("journal") {
                storeFile = rootProject.file(cle.getProperty("storeFile"))
                storePassword = cle.getProperty("storePassword")
                keyAlias = cle.getProperty("keyAlias")
                keyPassword = cle.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (fichierCle.exists()) signingConfig = signingConfigs.getByName("journal")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // le moteur est du Kotlin pur : il se teste sur la machine, sans téléphone ni émulateur
    testOptions { unitTests.isIncludeAndroidResources = false }

    buildFeatures { viewBinding = false }
}

dependencies {
    // Une seule dépendance, et une version stable : Material Components fournit les
    // puces filtre, la barre de navigation basse, le rail en paysage et le thème
    // DayNight. Elle amène AppCompat avec elle, rien d'autre à déclarer.
    // Volontairement PAS une version alpha : le composant ButtonGroup (remplaçant du
    // bouton segmenté déprécié) n'existe qu'en alpha, on utilise son prédécesseur
    // MaterialButtonToggleGroup, qui fait le même travail en stable.
    implementation("com.google.android.material:material:1.12.0")

    testImplementation("junit:junit:4.13.2")
}
