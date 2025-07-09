plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
}

// 依赖解析策略 - 解决annotations库版本冲突
configurations.all {
    resolutionStrategy {
        force("org.jetbrains:annotations:23.0.0")
        eachDependency {
            if (requested.group == "org.jetbrains" && requested.name == "annotations-java5") {
                useTarget("org.jetbrains:annotations:23.0.0")
                because("避免annotations库的重复类冲突")
            }
        }
    }
}

android {
    namespace = "com.github.turbomarkwon"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.github.turbomarkwon"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Markwon core
    implementation("io.noties.markwon:core:4.6.2")

    // 图片支持（Glide）- 排除冲突的annotations依赖
    implementation("com.github.bumptech.glide:glide:4.16.0") {
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }
    implementation("io.noties.markwon:image-glide:4.6.2")

    // 表格支持（使用正确的模块名）
    implementation("io.noties.markwon:ext-tables:4.6.2")

    // HTML 解析支持
    implementation("io.noties.markwon:html:4.6.2")

    // 任务列表支持
    implementation("io.noties.markwon:ext-tasklist:4.6.2")

    // 数学公式
    implementation("io.noties.markwon:ext-latex:4.6.2")

    // 行内解析插件（LaTeX行内公式需要）
    implementation("io.noties.markwon:inline-parser:4.6.2")

    // 语法高亮插件
    implementation("io.noties.markwon:syntax-highlight:4.6.2")

    // Prism4j 代码高亮引擎 - 语法高亮的核心依赖
    implementation("io.noties:prism4j:2.0.0")

    // Prism4j 注解处理器 - 用于生成语言包
    kapt("io.noties:prism4j-bundler:2.0.0")

    // 链接自动识别插件
    implementation("io.noties.markwon:linkify:4.6.2")
}