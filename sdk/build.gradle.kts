plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("signing")
    id("kotlin-parcelize")
}

android {
    namespace = "com.presagetech.smartspectra"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    sourceSets.getByName("main") {
        jniLibs {
            srcDirs("jni/")
        }
    }
    buildFeatures {
        buildConfig = false
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    publishing {
        singleVariant("release") {
            withJavadocJar()
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")

    implementation("com.jakewharton.timber:timber:5.0.1")

    val camerax_version = "1.3.4"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")

    // mediapipe dependencies
    implementation("com.google.flogger:flogger:0.6")
    implementation("com.google.flogger:flogger-system-backend:0.6")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.guava:guava:27.0.1-android")
    implementation("com.google.protobuf:protobuf-java:3.19.3")

    // mediapipe java library
    implementation(files("libs/classes.jar"))
}

task<Javadoc>("androidJavadoc") {
    exclude("**/R.html", "**/R.*.html", "**/index.html")
    options.encoding = "UTF-8"
}

signing {
    sign(publishing.publications)
}

publishing {
    repositories {
	maven {
	    credentials {
		      username = project.findProperty("MAVEN_USER") as String? ?: System.getenv("MAVEN_USER")
          password = project.findProperty("MAVEN_PASSWORD") as String? ?: System.getenv("MAVEN_PASSWORD")
	    }
	    url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
       }
    }
    publications {
        create<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
            groupId = "com.presagetech"
            artifactId = "smartspectra"
	    version = "1.0.11-SNAPSHOT"
            pom {
                name.set("Physiology SDK")
                description.set("Heart and respiration rate measurement by Presage Technologies")
                url.set("https://physiology.presagetech.com/")
            }
        }
    }
}
