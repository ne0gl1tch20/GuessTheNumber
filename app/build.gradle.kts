import org.jetbrains.kotlin.gradle.dsl.JvmTarget

import org.gradle.api.tasks.Copy
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    id("com.google.gms.google-services")
}

val generatedWebPlayAssets = layout.buildDirectory.dir("generated/webPlayAssets")
val generatedDebugBuildInfo = layout.buildDirectory.dir("generated/buildInfo/debug")
val generatedReleaseBuildInfo = layout.buildDirectory.dir("generated/buildInfo/release")
val appVersionName = "3.0.3"
val appVersionCode = 21
val generatedLauncherIconRes = layout.buildDirectory.dir("generated/launcherIconRes")
val launcherLogo = rootProject.file("GTN_APP_LOGO.png")
val prepareWebPlayBrandAsset by tasks.registering(Copy::class) {
    from(launcherLogo)
    into(generatedWebPlayAssets.map { it.dir("web-play") })
    rename { "gtn-app-logo.png" }
}
val prepareLauncherIcon by tasks.registering(Copy::class) {
    from(launcherLogo)
    into(generatedLauncherIconRes.map { it.dir("drawable-nodpi") })
    rename { "gtn_app_logo.png" }
}


fun registerBuildInfoTask(taskName: String, environmentName: String, outputDirectory: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>) =
    tasks.register(taskName) {
        outputs.dir(outputDirectory)
        doLast {
            fun gitValue(vararg arguments: String): String? = runCatching {
                val process = ProcessBuilder(*arguments)
                    .directory(rootProject.projectDir)
                    .redirectErrorStream(true)
                    .start()
                val value = process.inputStream.bufferedReader().use { it.readText().trim() }
                if (process.waitFor() == 0 && value.isNotBlank()) value else null
            }.getOrNull()

            val fullCommit = System.getenv("GITHUB_SHA")
                ?.takeIf { it.matches(Regex("[0-9a-fA-F]{40,64}")) }
                ?: gitValue("git", "rev-parse", "HEAD")
            val shortCommit = fullCommit?.take(8) ?: "unknown"
            val githubRef = System.getenv("GITHUB_REF").orEmpty()
            val branchCandidate = System.getenv("GITHUB_HEAD_REF")?.takeIf { it.isNotBlank() }
                ?: (if (githubRef.startsWith("refs/heads/")) System.getenv("GITHUB_REF_NAME") else null)
                ?: gitValue("git", "branch", "--show-current")
            val branch = branchCandidate
                ?.replace(Regex("[^A-Za-z0-9._-]"), "-")
                ?.trim('-')
                ?.ifBlank { "unknown" }
                ?: "unknown"
            val zone = ZoneId.of("Asia/Manila")
            val buildInstant = System.getenv("SOURCE_DATE_EPOCH")?.toLongOrNull()
                ?.let { Instant.ofEpochSecond(it).atZone(zone) }
                ?: ZonedDateTime.now(zone)
            val buildDate = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
                .format(buildInstant)
            val versionName = appVersionName
            val versionCode = appVersionCode
            val metadata = listOf(
                "name: GuessTheNumber-v$versionName-$shortCommit.$branch",
                "env: $environmentName",
                "git_commit: $shortCommit",
                "git_branch: $branch",
                "version_name: $versionName",
                "version_code: $versionCode",
                "build_date: $buildDate"
            ).joinToString(separator = "\n", postfix = "\n")
            val outputFile = outputDirectory.get().file("info.txt").asFile
            outputFile.parentFile.mkdirs()
            outputFile.writeText(metadata, Charsets.UTF_8)
            logger.lifecycle("Generated APK metadata at " + outputFile.relativeTo(rootProject.projectDir))
        }
    }

val generateDebugBuildInfo = registerBuildInfoTask("generateDebugBuildInfo", "debug", generatedDebugBuildInfo)
val generateReleaseBuildInfo = registerBuildInfoTask("generateReleaseBuildInfo", "release", generatedReleaseBuildInfo)


android {
    // Stable project debug signing. CI supplies the keystore through environment variables; local builds can use debug-signing/debug.keystore.
    val ciDebugKeystore = System.getenv("GTN_DEBUG_KEYSTORE_PATH")?.let(::file)
    val projectDebugKeystore = rootProject.file("debug-signing/debug.keystore")
    val selectedDebugKeystore = ciDebugKeystore?.takeIf { it.isFile } ?: projectDebugKeystore
    val hasProjectDebugKeystore = selectedDebugKeystore.isFile
    val debugStorePassword = System.getenv("GTN_DEBUG_KEYSTORE_PASSWORD") ?: "android"
    val debugKeyAlias = System.getenv("GTN_DEBUG_KEY_ALIAS") ?: "guessthenumber-debug"
    val debugKeyPassword = System.getenv("GTN_DEBUG_KEY_PASSWORD") ?: "android"

    namespace = "com.jarrlyyy.guessthenumber"
    compileSdk = 37

    defaultConfig {
        val liveOpsManifestUrl = providers.gradleProperty("luaLiveOpsManifestUrl").orElse("https://raw.githubusercontent.com/ne0gl1tch20/GuessTheNumber-LiveOps/main/manifest.json").get()
        // The base64-encoded DER key may be line-wrapped in GitHub Actions secrets.
        // Remove whitespace before embedding it so generated BuildConfig.java stays valid
        // and Android's Base64 decoder receives only the encoded key bytes.
        val liveOpsPublicKey = providers.gradleProperty("luaLiveOpsPublicKeyBase64")
            .orElse("")
            .get()
            .filterNot(Char::isWhitespace)
        buildConfigField("String", "LIVEOPS_MANIFEST_URL", "\"$liveOpsManifestUrl\"")
        buildConfigField("String", "LIVEOPS_PUBLIC_KEY_BASE64", "\"$liveOpsPublicKey\"")
        val buildCommitSha = System.getenv("GITHUB_SHA") ?: "unknown"
        buildConfigField("String", "GIT_COMMIT_SHA", "\"$buildCommitSha\"")
        val stableDebugBuildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toLongOrNull() ?: 0L
        buildConfigField("long", "STABLE_DEBUG_BUILD_NUMBER", "$stableDebugBuildNumber")
        applicationId = "com.jarrlyyy.guessthenumber"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasProjectDebugKeystore) {
            create("projectDebug") {
                storeFile = selectedDebugKeystore
                storePassword = debugStorePassword
                keyAlias = debugKeyAlias
                keyPassword = debugKeyPassword
            }
        }
    }

    buildTypes {
        release {
            buildConfigField("String", "UPDATE_CHANNEL", "\"release\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "rules.pro"
            )
            if (hasProjectDebugKeystore) {
                signingConfig = signingConfigs.getByName("projectDebug")
            }
        }
        debug {
            buildConfigField("String", "UPDATE_CHANNEL", "\"debug\"")
            applicationIdSuffix = ".debug"
            isDebuggable = true
            if (hasProjectDebugKeystore) {
                signingConfig = signingConfigs.getByName("projectDebug")
            }
        }
    }
    // Android's SourceSet API no longer accepts Provider instances directly.
    sourceSets.getByName("main").assets.srcDir(generatedWebPlayAssets.get().asFile)
    sourceSets.getByName("debug").assets.srcDir(generatedDebugBuildInfo.get().asFile)
    sourceSets.getByName("release").assets.srcDir(generatedReleaseBuildInfo.get().asFile)
    sourceSets.getByName("main").res.srcDir(generatedLauncherIconRes.get().asFile)
    // Generate the web brand asset and real launcher icon before Android resource processing.
    tasks.matching { it.name == "preBuild" || it.name.endsWith("PreBuild") }
        .configureEach { dependsOn(prepareWebPlayBrandAsset, prepareLauncherIcon) }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    ndkVersion = "30.0.16248370"
    buildToolsVersion = "36.0.0"
}

tasks.named("preBuild").configure {
    dependsOn(prepareWebPlayBrandAsset, prepareLauncherIcon)
}

tasks.matching { it.name == "preDebugBuild" || it.name == "mergeDebugAssets" }.configureEach {
    dependsOn(generateDebugBuildInfo)
}
tasks.matching { it.name == "preReleaseBuild" || it.name == "mergeReleaseAssets" }.configureEach {
    dependsOn(generateReleaseBuildInfo)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform("com.google.firebase:firebase-bom:35.0.2"))
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.luaj.jme)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.media)
    implementation(libs.androidx.webkit)

    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.15")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
