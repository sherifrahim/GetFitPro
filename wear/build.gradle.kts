plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.getfit.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.getfit.wear"
        // Wear OS 3+ only (API 30) — required for the Health Services ExerciseClient API used for
        // live heart rate; the phone app's minSdk (26) doesn't apply to this module.
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

// One of the AndroidX BOM alignment platforms on this module's classpath publishes a `strictly`
// constraint that substitutes com.google.guava:listenablefuture:1.0 (the real stub, containing the
// actual ListenableFuture class) with 9999.0-empty-to-avoid-conflict-with-guava (an intentionally
// empty jar, meant only for when full Guava is also present). Confirmed via
// `.\gradlew :wear:dependencies --configuration debugCompileClasspath`, which showed
// "com.google.guava:listenablefuture:1.0 -> 9999.0-empty-to-avoid-conflict-with-guava". A normal
// dependency declaration can't outrank a `strictly` constraint from upstream metadata, so it has to
// be forced here.
configurations.all {
    resolutionStrategy {
        force("com.google.guava:listenablefuture:1.0")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.androidx.wear.compose.foundation)

    implementation(libs.play.services.wearable)
    // health-services-client transitively pulls in the FULL com.google.guava:guava:31.1-android jar,
    // which bundles its own copy of ListenableFuture. Normally Guava's own metadata routes that
    // bundled copy around the standalone listenablefuture stub to avoid a duplicate — but our forced
    // resolution below (needed so the real class is available at compile time; see that comment)
    // defeats that, producing a real "Duplicate class ListenableFuture" failure at package time
    // (confirmed via `.\gradlew :wear:dependencies --configuration debugRuntimeClasspath`, which
    // showed full guava:31.1-android as a direct dependency of health-services-client). We only ever
    // need the bare ListenableFuture interface, nothing else full Guava provides, so drop it here and
    // let the small stub below supply the class everywhere instead.
    implementation(libs.androidx.health.services.client) {
        exclude(group = "com.google.guava", module = "guava")
    }
    // ExerciseClient methods return Guava's ListenableFuture; Health Services doesn't expose the
    // class to this module's compile classpath on its own, so it's declared directly. This is the
    // small purpose-built stub (just the ListenableFuture interface), not the full Guava library.
    implementation(libs.guava.listenablefuture)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.ui.tooling)
}
