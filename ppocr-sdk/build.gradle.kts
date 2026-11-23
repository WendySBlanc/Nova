plugins {
    id("mihon.library")
    kotlin("android")
}

android {
    namespace = "com.paddle.ocr"

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            consumerProguardFiles("proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(libs.onnxruntime.android)
    implementation(libs.opencv.android)
    implementation(kotlinx.coroutines.android)
    implementation(androidx.corektx)

    androidTestImplementation(androidx.test.ext)
    androidTestImplementation(androidx.test.runner)
}
