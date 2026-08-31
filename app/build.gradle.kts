import java.util.Properties

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.hilt)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.kover)
}

// Load properties from .env and local.properties
val projectConfig = Properties().apply {
  // Order matters: later files override earlier ones
  listOf("local.properties", ".env").forEach { fileName ->
    val file = rootProject.file(fileName)
    if (file.exists()) {
      file.inputStream().use { load(it) }
    }
  }
}
fun getConfig(key: String, default: String): String {
  return projectConfig.getProperty(key) ?: System.getenv(key) ?: default
}

android {
  namespace = "hu.muzso.android_system_dumper"
  compileSdk = 37

  defaultConfig {
    minSdk = 26

    testInstrumentationRunner = "hu.muzso.android_system_dumper.HiltTestRunner"

    // Various network timeouts (connect, read, write, etc.).
    buildConfigField("long", "NETWORK_TIMEOUT_MS", getConfig("NETWORK_TIMEOUT_MS", "30000"))
    // For demonstration purposes you can hardwire the IP address (that is shown on screen) here:
    buildConfigField("String", "HTTP_SERVER_IP_ADDRESS", "\"${getConfig("HTTP_SERVER_IP_ADDRESS", "")}\"")
    // And the TCP port that the HTTP server listens on:
    buildConfigField("int", "HTTP_SERVER_TCP_PORT", getConfig("HTTP_SERVER_TCP_PORT", "0"))
    // Useful if e.g. you run this app in an emulator and you can reach it only through port forwarding.

    // The third-party TorService requires this.
    // See: https://github.com/guardianproject/tor-android/blob/master/sampletorapp/build.gradle.kts
    compileOptions {
      sourceCompatibility = JavaVersion.VERSION_24
      targetCompatibility = JavaVersion.VERSION_24
    }
    javaCompileOptions {
      annotationProcessorOptions {
        arguments["moshi.generateAdapter.kapt.deprecation"] = "false"
      }
    }

    externalNativeBuild {
      cmake {
        abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
      }
    }
    ndk {
      abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
    }
  }

  buildTypes {
    release {
      // something about either minification or shrinking or obfuscation breaks Tor
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      buildConfigField("int", "BATCH_LIMIT", getConfig("BATCH_LIMIT", "0"))
      buildConfigField("int", "FILE_COUNT_LIMIT", getConfig("FILE_COUNT_LIMIT", "0"))
      buildConfigField("boolean", "LOG_TO_SYSTEM", getConfig("LOG_TO_SYSTEM", "false"))
    }
    debug {
      buildConfigField("int", "BATCH_LIMIT", getConfig("BATCH_LIMIT", "1"))
      buildConfigField("int", "FILE_COUNT_LIMIT", getConfig("FILE_COUNT_LIMIT", "1000"))
      buildConfigField("boolean", "LOG_TO_SYSTEM", getConfig("LOG_TO_SYSTEM", "true"))
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
  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
    }
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      all {
        it.useJUnitPlatform()
      }
    }
  }
}

ksp {
  arg("moshi.generateAdapter.kapt.deprecation", "false")
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("NETWORK_TIMEOUT_MS")
  ignoreList.add("HTTP_SERVER_IP_ADDRESS")
  ignoreList.add("HTTP_SERVER_TCP_PORT")
  ignoreList.add("BATCH_LIMIT")
  ignoreList.add("FILE_COUNT_LIMIT")
  ignoreList.add("LOG_TO_SYSTEM")
}

kover {
  reports {
    total {
      log {
        onCheck = true
      }
      xml {
        onCheck = true
      }
      html {
        onCheck = true
      }
      verify {
        rule("Quality Gate") {
          minBound(80)
        }
      }
    }
    filters {
      includes {
        packages("hu.muzso.android_system_dumper")
      }
      excludes {
        packages("hu.muzso.android_system_dumper.di")
        classes(
          "hu.muzso.android_system_dumper.BuildConfig",
          "hu.muzso.android_system_dumper.HiltTestRunner",
          "hu.muzso.android_system_dumper.upload.network.Gofile*",
          "hu.muzso.android_system_dumper.MainActivity",
          "hu.muzso.android_system_dumper.platform.CustomTorService",
          "**.Hilt_*",
          "**.*_Factory",
          "**.*_HiltModules*",
          "**.*_MembersInjector",
          "**.*_Provide*Factory",
          "**.*_GeneratedInjector",
          "**.*JsonAdapter",
          "**.*_Impl",
          "**.*ComposableSingletons*",
          "**.*ComposableLambda*",
          "**.*$*"
        )
        annotatedBy("javax.annotation.processing.Generated")
      }
    }
  }
}

@Suppress("DuplicateDependency")
dependencies {
  implementation(project(":domain"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.apache.commons.validator)
  implementation(libs.converter.moshi)
  implementation(libs.hilt.android)
  implementation(libs.hilt.navigation.compose)
  implementation(libs.jtorctl)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.tor.android)
  implementation(libs.zxing)
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
  implementation(libs.ktor.server.html.builder)

  ksp(libs.androidx.room.compiler)
  ksp(libs.hilt.compiler)
  ksp(libs.moshi.kotlin.codegen)

  testImplementation(testFixtures(project(":domain")))
  testImplementation(platform(libs.androidx.compose.bom))
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.hilt.android.testing)
  testImplementation(libs.junit)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.vintage.engine)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.ktor.server.test.host)
  testImplementation(libs.mockk)
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  testImplementation(libs.truth)
  testImplementation(libs.turbine)

  testRuntimeOnly(libs.junit.platform.launcher)

  androidTestImplementation(testFixtures(project(":domain")))
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.accessibility)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.junit.ktx)
  androidTestImplementation(libs.androidx.rules)
  androidTestImplementation(libs.androidx.runner)
  androidTestImplementation(libs.androidx.test.core.ktx)
  androidTestImplementation(libs.hilt.android.testing)
  androidTestImplementation(libs.mockk.android)
  androidTestImplementation(libs.okhttp.mockwebserver)
  androidTestImplementation(libs.truth)

  debugImplementation(platform(libs.androidx.compose.bom))
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}

// Workaround for the "Kapt support in Moshi Kotlin Code Gen is deprecated and will be removed in 2.0. Please migrate to KSP." warning.
// See: https://github.com/square/moshi/discussions/1752
tasks.withType<JavaCompile>().configureEach {
  if (name.startsWith("hiltJavaCompile")) {
    doFirst {
      options.annotationProcessorPath =
        options.annotationProcessorPath?.filter {
          !it.name.startsWith("moshi-kotlin-codegen-")
        }
    }
  }
}

// We perform the strict pixel-by-pixel screenshot verification on every "check" run.
// The ground truth (aka. "golden" images) is in "app/src/test/screenshots".
// They can be updated (regenerated) via `./gradlew recordRoborazziDebug`.
tasks.named("check") {
  dependsOn("verifyRoborazziDebug")
}
