plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
}

android {
    namespace = "work.delsart.modalbottomsheet"
    compileSdk = 33

    defaultConfig {
        minSdk = 26
        aarMetadata {
            minCompileSdk = 29
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
            register<MavenPublication>("release") {
                groupId = "work.delsart.bottomsheet"
                artifactId = "bottomsheet"
                version = "0.0.1"
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