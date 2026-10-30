plugins {
    alias(libs.plugins.android.application)
}

kotlin {
    jvmToolchain(26)
}

android {
    namespace = "tv.wtv.app"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "tv.wtv.app"
        minSdk = 31
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
        sourceCompatibility = JavaVersion.VERSION_26
        targetCompatibility = JavaVersion.VERSION_26
    }
}

val webDist = rootProject.layout.projectDirectory.dir("web/dist")
val webAssets = layout.buildDirectory.dir("generated/webAssets")

val requireWebDist by tasks.registering {
    doLast {
        if (!webDist.asFile.isDirectory) {
            throw GradleException("web/dist is missing. Run `npm ci && npm run build` in web/ first.")
        }
    }
}

val copyWebDist by tasks.registering(Sync::class) {
    dependsOn(requireWebDist)
    from(webDist)
    into(webAssets)
}

android.sourceSets
    .getByName("main")
    .assets.directories
    .add(webAssets.get().asFile.path)

tasks.configureEach {
    if (name.startsWith("merge") && name.endsWith("Assets")) dependsOn(copyWebDist)
}

dependencies {
    implementation(libs.androidx.webkit)
    testImplementation(libs.junit)
}
