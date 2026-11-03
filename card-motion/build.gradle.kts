plugins {
    id("mihon.library")
    id("mihon.library.compose")
    kotlin("android")
}

android {
    namespace = "nova.cardmotion"
}

dependencies {
    implementation(compose.activity)
    implementation(compose.animation)
    implementation(compose.foundation)
    implementation(compose.ui.util)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
