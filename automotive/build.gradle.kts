plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.hilt)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.kover)
}

android {
  namespace = "hu.muzso.android_system_dumper.automotive"
  compileSdk = 37

  defaultConfig {
    applicationId = "hu.muzso.android_system_dumper.automotive"
    minSdk = 26
    targetSdk = 37
    versionCode = 6
    versionName = "1.1.3"

    testInstrumentationRunner = "hu.muzso.android_system_dumper.HiltTestRunner"
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // something about either minification or shrinking or obfuscation breaks Tor
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    buildConfig = true
  }
  packaging {
    resources {
      excludes += "/META-INF/INDEX.LIST"
      excludes += "/META-INF/io.netty.versions.properties"
    }
  }
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
    }
    filters {
      includes {
        packages("hu.muzso.android_system_dumper")
      }
      excludes {
        classes(
          "hu.muzso.android_system_dumper.automotive.BuildConfig",
          "hu.muzso.android_system_dumper.AutomotiveApplication",
          "hu.muzso.android_system_dumper.HiltTestRunner",
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

dependencies {
  implementation(project(":app"))
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
}

