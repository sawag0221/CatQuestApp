plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)

//    id("com.google.devtools.ksp") version "2.2.0-2.0.2"

//    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
    // Existing plugins
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.devtools.ksp) // ★ バージョン指定なしでエイリアスを使用

//    id("com.google.devtools.ksp") // ★ これが必要

//    id("com.android.application")

    id("com.google.gms.google-services")
}

android {
    namespace = "com.sawag.catquestapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sawag.catquestapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
//    kotlinOptions {
//        jvmTarget = "1.8"
//    }
    buildFeatures {
        compose = true
    }
//    composeOptions {
//        kotlinCompilerExtensionVersion = "1.5.1"
//    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // ★★★ このブロックを追加または修正 ★★★
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {


    // Room

    implementation(libs.androidx.room.runtime)
    // annotationProcessor("androidx.room:room-compiler:$roomVersion") // ← KSPを使う場合はコメントアウトまたは削除
    ksp(libs.androidx.room.compiler)              // ← KSPを使用
    implementation(libs.androidx.room.ktx)
    testImplementation(libs.androidx.room.testing)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
//    implementation(libs.androidx.navigation.compose.jvmstubs)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // ... 他の依存関係 ...
    implementation(libs.androidx.navigation.compose) // 最新バージョンを確認してください
    implementation(libs.androidx.foundation) // Compose Foundation のバージョンを確認
    implementation(libs.kotlinx.serialization.json)

    implementation("com.google.code.gson:gson:2.13.1") // 最新バージョンを確認してください

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))


    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.google.firebase:firebase-auth-ktx")


    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries
}
kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}