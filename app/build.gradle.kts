import java.util.Properties
import java.io.FileInputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

val localProperties = Properties().apply {
  val localFile = rootProject.file("local.properties")
  if (localFile.exists()) {
    load(FileInputStream(localFile))
  }
}

val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") 
  ?: System.getenv("GEMINI_API_KEY") 
  ?: ""

val firebaseApiKey = localProperties.getProperty("FIREBASE_API_KEY")
  ?: System.getenv("FIREBASE_API_KEY")
  ?: ""

val firebaseProjectId = localProperties.getProperty("FIREBASE_PROJECT_ID")
  ?: System.getenv("FIREBASE_PROJECT_ID")
  ?: ""

val firebaseAuthDomain = localProperties.getProperty("FIREBASE_AUTH_DOMAIN")
  ?: System.getenv("FIREBASE_AUTH_DOMAIN")
  ?: ""

val firebaseAppId = localProperties.getProperty("FIREBASE_APP_ID")
  ?: System.getenv("FIREBASE_APP_ID")
  ?: ""

val firebaseWebClientId = localProperties.getProperty("FIREBASE_WEB_CLIENT_ID")
  ?: System.getenv("FIREBASE_WEB_CLIENT_ID")
  ?: ""

val keystorePass = localProperties.getProperty("KEYSTORE_PASSWORD")
  ?: System.getenv("KEYSTORE_PASSWORD")
  ?: ""

val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
val dynamicVersionCode = 2000 + runNumber

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.gsd.gtoolx"
    minSdk = 26
    targetSdk = 34
    versionCode = dynamicVersionCode
    versionName = "2.6.$runNumber"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    buildConfigField("String", "FIREBASE_API_KEY", "\"$firebaseApiKey\"")
    buildConfigField("String", "FIREBASE_PROJECT_ID", "\"$firebaseProjectId\"")
    buildConfigField("String", "FIREBASE_AUTH_DOMAIN", "\"$firebaseAuthDomain\"")
    buildConfigField("String", "FIREBASE_APP_ID", "\"$firebaseAppId\"")
    buildConfigField("String", "FIREBASE_WEB_CLIENT_ID", "\"$firebaseWebClientId\"")
  }

  signingConfigs {
    create("release") {
      storeFile = if (file("release.keystore").exists()) file("release.keystore") else file("${rootDir}/release.keystore")
      storePassword = keystorePass
      keyAlias = "gtoolx_release_key"
      keyPassword = keystorePass
    }
    getByName("debug") {
      // Strictly points to the committed static keystore file
      storeFile = if (file("debug.keystore").exists()) file("debug.keystore") else file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
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
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.mlkit.text.recognition)
  implementation(libs.mlkit.document.scanner)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation(libs.zxing.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
