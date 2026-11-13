plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Same version as the phone app, so the two always ship together.
val appVersionName = "0.2.0"
val appVersionCode = appVersionName.split(".").let { (a, b, c) -> a.toInt() * 10000 + b.toInt() * 100 + c.toInt() }

android {
    namespace = "io.github.taehunkim.dorangori.wear"
    compileSdk = 37

    defaultConfig {
        // Must equal the phone app's applicationId for the Data Layer to pair the two.
        applicationId = "io.github.taehunkim.dorangori"
        minSdk = 30
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // Release signing: the key lives outside the repo (see docs/development.md). OndolSaem, Nunsori, Jogakbo and Malbotdari let
    // apps signed with their own key in without asking, so release builds share the Nunsori release key.
    fun signingProp(name: String): String? = (project.findProperty(name) as String?) ?: System.getenv(name)
    val releaseStore = signingProp("DORANGORI_STORE_FILE")
    if (releaseStore != null) {
        signingConfigs.create("release") {
            storeFile = file(releaseStore)
            storePassword = signingProp("DORANGORI_STORE_PASSWORD")
            keyAlias = signingProp("DORANGORI_KEY_ALIAS")
            keyPassword = signingProp("DORANGORI_KEY_PASSWORD")
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
    }
    buildFeatures { compose = true; buildConfig = true }
    androidResources { generateLocaleConfig = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.ongoing)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.play.services.wearable)
    constraints {
        // Play services base brings Fragment 1.1.0; ActivityResult permission handling needs 1.3+.
        implementation("androidx.fragment:fragment:1.8.9")
    }
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
