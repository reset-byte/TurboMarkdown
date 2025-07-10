plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
        viewBinding = true
    }
}

dependencies {
    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // UI Components
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

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

    // 语法高亮插件 - 暂时禁用
    // implementation("io.noties.markwon:syntax-highlight:4.6.2")

    // Prism4j 代码高亮引擎 - 暂时禁用
    // implementation("io.noties:prism4j:2.0.0")

    // Prism4j 注解处理器 - 暂时禁用
    // kapt("io.noties:prism4j-bundler:2.0.0")

    // 链接自动识别插件
    implementation("io.noties.markwon:linkify:4.6.2")
    
    // RecyclerView adapter
    implementation("io.noties.markwon:recycler:4.6.2")
    implementation("io.noties.markwon:recycler-table:4.6.2")
}