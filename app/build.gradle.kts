plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "tv.wtv.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "tv.wtv.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }

}

// Build the web app and copy output into Android assets before compilation.
val buildWebApp by tasks.registering(Exec::class) {
    workingDir = rootProject.file("web")
    commandLine("npm", "run", "build")
    inputs.dir(rootProject.file("web/src"))
    inputs.file(rootProject.file("web/index.html"))
    inputs.file(rootProject.file("web/app.css"))
    inputs.file(rootProject.file("web/package.json"))
    outputs.dir(rootProject.file("web/dist"))
}

val copyWebAssets by tasks.registering(Copy::class) {
    dependsOn(buildWebApp)
    from(rootProject.file("web/dist"))
    into(layout.projectDirectory.dir("src/main/assets"))
}

tasks.named("preBuild") {
    dependsOn(copyWebAssets)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
}
