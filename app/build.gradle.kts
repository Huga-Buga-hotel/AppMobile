plugins {
    alias(libs.plugins.android.application)
}

/**
 * URL da API. Debug: o backend no seu computador visto pelo emulador (10.0.2.2).
 * Release (AWS): passe a URL pública, ex.:
 *   ./gradlew assembleRelease -Phugabuga.apiUrl=https://api.seudominio.com.br/api/v1/
 * ou coloque hugabuga.apiUrl=... no ~/.gradle/gradle.properties.
 */
val apiUrlDebug = (findProperty("hugabuga.apiUrlDebug") as String?) ?: "http://10.0.2.2:8080/api/v1/"
val apiUrlRelease = (findProperty("hugabuga.apiUrl") as String?) ?: "https://api.hugabuga.example/api/v1/"

android {
    namespace = "com.example.hugabugahotel"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.hugabugahotel"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$apiUrlDebug\"")
            // HTTP sem TLS só no desenvolvimento (backend local)
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$apiUrlRelease\"")
            // Em produção a API precisa estar em HTTPS
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            res.directories.addAll(
                listOf(
                    "src/main/res",
                    "src/main/res-home",
                    "src/main/res-caverns",
                    "src/main/res-experiences",
                    "src/main/res-checkout",
                    "src/main/res-account",
                    "src/main/res-states",
                )
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.google.material)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.coil)
    implementation(libs.zxing.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
