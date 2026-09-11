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
        // MUST match the phone app's applicationId. The Wearable Data Layer delivers MessageClient/
        // DataClient traffic only to the app with the SAME package name on the other device (and
        // Play bundles a Wear app under the phone listing by the same rule). With "com.getfit.wear"
        // here, every snapshot the phone sent on a real Galaxy Watch went nowhere — silently, no log
        // on either side. The Kotlin namespace stays com.getfit.wear; only the installed id changes.
        applicationId = "com.getfit"
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

// Read the Guava note on health-services-client below before touching this. The scope of this
// force is load-bearing: COMPILE classpath only, never runtime.
//
// An AndroidX alignment platform publishes a `strictly` constraint substituting
// com.google.guava:listenablefuture:1.0 (the real stub, holding the actual ListenableFuture class)
// with 9999.0-empty-to-avoid-conflict-with-guava (a deliberately EMPTY jar). That substitution is
// correct at runtime — full Guava is there and already supplies the class, so the empty jar is what
// keeps it from being a duplicate. But it also empties our compileOnly stub, leaving the compile
// classpath with no ListenableFuture at all (health-services-client declares Guava as
// `implementation`, so it doesn't hand us one). Forcing the real 1.0 back for compilation only
// fixes that without putting a second copy of the class into the APK.
//
// This was previously `configurations.all`, which applied the force to the runtime classpath too
// and produced a genuine "Duplicate class ListenableFuture" at package time. Excluding full Guava
// to silence that then stripped com.google.common.base.Preconditions, which Health Services calls
// internally, crashing the watch app on launch. Keep the force narrow instead.
configurations.matching { it.name.endsWith("CompileClasspath") }.configureEach {
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
    // health-services-client transitively brings the FULL com.google.guava:guava jar. Leave it
    // there. It supplies both ListenableFuture (which ExerciseClient's *Async methods return) and
    // com.google.common.base.Preconditions, which Health Services' own internal IPC layer calls
    // from ConnectionConfiguration.<init> on the very first HealthServices.getClient().
    //
    // Do NOT re-add either of these, both of which were here before and are why the watch app
    // crashed on launch with NoClassDefFoundError: Preconditions:
    //   * force("com.google.guava:listenablefuture:1.0")
    //   * exclude(group = "com.google.guava", module = "guava") on this dependency
    // The standalone listenablefuture artifact resolving to "9999.0-empty-to-avoid-conflict-with-
    // guava" is not a problem to be fought — it is an intentionally EMPTY jar, and it is precisely
    // the mechanism that prevents a duplicate ListenableFuture when full Guava is present. Forcing
    // it back to the real 1.0 stub re-introduced that duplicate; excluding full Guava to silence
    // *that* then removed Preconditions and broke the app at runtime. Nothing in this module names
    // compile time either. Default runtime resolution is correct on its own.
    implementation(libs.androidx.health.services.client)
    // ...with one exception, which must stay compileOnly. health-services-client declares Guava as
    // `implementation`, so it is on our RUNTIME classpath but not our COMPILE classpath, and Kotlin
    // still has to resolve ExerciseClient.startExerciseAsync's ListenableFuture return type to
    // type-check the call — even though we discard the value. compileOnly puts the interface on the
    // compile classpath without packaging it, so it cannot duplicate full Guava's copy in the APK.
    // Anything stronger than compileOnly here (implementation/api) re-creates the duplicate-class
    // failure at package time.
    compileOnly(libs.guava.listenablefuture)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.ui.tooling)
}
