plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("app.cash.sqldelight")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("io.ktor:ktor-client-core:2.3.6")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
                implementation("app.cash.sqldelight:primitive-adapters:2.0.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
                implementation("com.google.android.gms:play-services-fitness:21.1.0")
                implementation("com.google.android.gms:play-services-auth:20.7.0")
                implementation("app.cash.sqldelight:android-driver:2.0.1")
            }
        }
        
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.0.2")
            }
        }
    }
}

android {
    namespace = "com.stepquest.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("StepQuestDatabase") {
            packageName.set("com.stepquest.data.db")
        }
    }
}
