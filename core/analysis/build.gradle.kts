plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "br.com.reverselab.core.analysis"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:domain"))
}
