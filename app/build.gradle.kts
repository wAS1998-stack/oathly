import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Google's official test IDs: always safe for development builds.
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerId = "ca-app-pub-3940256099942544/9214589741"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"

/** Gradle property or environment variable; blank values count as missing. */
fun prop(name: String, fallback: String): String {
    val value = providers.gradleProperty(name).orNull ?: providers.environmentVariable(name).orNull
    return if (value.isNullOrBlank()) fallback else value
}

val keystore = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.oathly.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.oathly.app"
        minSdk = 26
        targetSdk = 36
        versionCode = prop("OATHLY_VERSION_CODE", "1").toInt()
        versionName = "1.0.0"
        buildConfigField("String", "DATA_BASE_URL", "\"${prop("OATHLY_DATA_BASE_URL", "https://oathly.pages.dev/v1/")}\"")
    }

    signingConfigs {
        if (keystore.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testBannerId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$testInterstitialId\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["admobAppId"] = prop("OATHLY_ADMOB_APP_ID", testAppId)
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${prop("OATHLY_ADMOB_BANNER_ID", testBannerId)}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${prop("OATHLY_ADMOB_INTERSTITIAL_ID", testInterstitialId)}\"")
            if (keystore.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.billing)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
