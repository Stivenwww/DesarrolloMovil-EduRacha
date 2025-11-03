plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
}


android {
    namespace = "com.stiven.sos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.stiven.sos"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = false
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            // Se mantienen tus exclusiones para evitar conflictos
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/DEPENDENCIES"
            )
        }
    }
}

dependencies {

    // -------------------
    // CORE Y ANDROIDX
    // -------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Desugarización para usar APIs modernas de Java en versiones antiguas de Android
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // -------------------
    // JETPACK COMPOSE
    // -------------------
    // BOM (Bill of Materials) para gestionar versiones de Compose de forma consistente
    implementation(platform("androidx.compose:compose-bom:2024.05.00")) // Versión más reciente
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Componentes de Arquitectura para Compose
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // -------------------
    // FIREBASE
    // -------------------
    // BOM de Firebase para gestionar versiones
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    // Dependencias específicas de Firebase (no es necesario añadir -ktx por separado)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database-ktx") //  Para Realtime Database

    // -------------------
    // GOOGLE SIGN-IN & CREDENTIALS
    // -------------------
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)

    // -------------------
    // RED (NETWORKING)
    // -------------------
    // Retrofit (Cliente HTTP)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // El interceptor de logging es muy útil

    // Ktor (Alternativa a Retrofit, ya lo tenías)
    implementation("io.ktor:ktor-client-core:2.3.11") // Versiones actualizadas
    implementation("io.ktor:ktor-client-cio:2.3.11")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    // Serialización para Ktor
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")

    // -------------------
    // CORRUTINAS
    // -------------------
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0") //  Para integrar con Firebase/Play Services

    // -------------------
    // OTRAS LIBRERÍAS
    // -------------------
    implementation("io.coil-kt:coil-compose:2.6.0") // Carga de imágenes en Compose
    implementation("com.google.code.gson:gson:2.10.1") // Conversor JSON, útil para Retrofit y otros

    // App Distribution (no es una dependencia de la app, sino del plugin de Gradle)

    // -------------------
    // TESTING
    // -------------------
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
