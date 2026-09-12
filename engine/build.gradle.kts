// Pure-Kotlin (JVM) module shared by :app and :wear.
//
// Holds the guided-session state machine and the formatters it needs, and nothing Android. The
// watch runs the SAME engine as the phone for standalone workouts — one state machine, one set of
// tests (they live in app/src/test and compile against this module), no second copy to drift.
// Keep this module free of Android and of anything with a heavier dependency than
// kotlinx-serialization; if something needs Context, it doesn't belong here.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
