plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.moutamid.emailfetcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.moutamid.emailfetcher"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { viewBinding = true }
    packaging {
        resources.pickFirsts.add("META-INF/*")
        resources.excludes.add("META-INF/*.LIST") // Excludes all files with .LIST extension in META-INF
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("com.fxn769:stash:1.3.2")
    implementation("com.google.apis:google-api-services-gmail:v1-rev20220404-2.0.0")
//    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
//    implementation("com.google.guava:guava:31.1-android")

    implementation("com.google.android.gms:play-services-auth:21.1.0")
//    implementation("com.google.android.gms:play-services-auth:15.0.0")
    implementation("com.google.api-client:google-api-client:2.4.0")
    implementation("com.google.api-client:google-api-client-android:1.33.2") {
        exclude("org.apache.httpcomponents")
    }
    implementation("com.google.http-client:google-http-client-gson:1.19.0"){
        exclude("httpclient")
        exclude("com.google.android", "android")
    }
//    implementation("com.google.httpclient:google-http-client-android:1.43.2")
//    implementation("com.google.api.client:json-jackson2-android:1.43.2")
//    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")

    implementation("pub.devrel:easypermissions:1.2.0")
//    implementation("com.sun.mail:android-mail:1.6.0")
//    implementation("com.sun.mail:android-activation:1.6.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}