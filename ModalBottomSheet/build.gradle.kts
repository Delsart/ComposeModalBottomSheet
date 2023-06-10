plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "work.delsart.modalbottomsheet"
    compileSdk = 33

    testFixtures {
        enable = true
    }
    defaultConfig {
        minSdk = 26
        aarMetadata {
            minCompileSdk = 33
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.7"
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }


}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("maven_public") {
                groupId = "work.delsart.bottomSheet"
                artifactId = "bottomSheet"
                version = "v0.0.1"
                afterEvaluate {
                    from(components["release"])
                }
            }

        }
    }
}


dependencies {
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation(platform("androidx.compose:compose-bom:2023.05.00"))

}