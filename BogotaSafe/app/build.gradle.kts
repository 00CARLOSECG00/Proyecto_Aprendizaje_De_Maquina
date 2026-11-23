import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.bogotasafe"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.bogotasafe"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Cargar variables desde local.properties y asignarlas a BuildConfig
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }
        val mapTilerKey = localProperties.getProperty("MAPTILER_API_KEY") ?: ""
        buildConfigField("String", "MAPTILER_API_KEY", "\"$mapTilerKey\"")
    }

    buildTypes {
        release {
            // Firma con la llave de depuración para poder instalar la versión optimizada en el
            // celular de pruebas. Para publicar en Play Store se debe usar una llave propia.
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // 1. Librerías añadidas para GPS en tiempo real y Mapa Interactivo
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Play Services trae una versión vieja de Fragment; se fuerza una actual (permisos en tiempo de ejecución)
    implementation("androidx.fragment:fragment:1.9.1")
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    // Efecto Liquid Glass (refracción y desenfoque real)
    implementation("dev.chrisbanes.haze:haze-glass:2.1.0")

    // 2. Dependencias base de Android y Jetpack Compose (Sin duplicados)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // 3. Testing y Debug
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}