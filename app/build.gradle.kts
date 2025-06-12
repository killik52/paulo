plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.myapplication" // Este é o namespace que define o pacote do BuildConfig
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Habilitar minificação
            isShrinkResources = true // Habilitar redução de recursos
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro" // Certifique-se que este arquivo existe e tem as regras necessárias
            )
        }
        debug {
            // Configurações específicas para debug (opcional, mas bom para desenvolvimento)
            applicationIdSuffix = ".debug" // Sufixo para diferenciar o ID do app em debug
            isDebuggable = true

            // Para debug, você pode querer desabilitar a minificação para facilitar a depuração
            // isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true // Habilita o ViewBinding
        buildConfig = true // Garante que BuildConfig.APPLICATION_ID esteja disponível
    }
}

dependencies {
    // Dependências do AndroidX e Material Design (Assumindo que 'libs' refere-se a um catálogo de versões)
    implementation(libs.material)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.activity) // Ex: androidx.activity:activity-ktx

    // >>>>> ADICIONE ESTA LINHA <<<<<
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.1")

    // Retrofit e Gson para APIs Web (MANTER PARA A FUNCIONALIDADE DE CNPJ)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    // logging-interceptor apenas para builds de debug (MANTER PARA A FUNCIONALIDADE DE CNPJ)
    debugImplementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Outras dependências do seu projeto (que não eram exclusivas da funcionalidade de notícias)
    implementation("com.google.mlkit:text-recognition:16.0.0") // Para OCR
    implementation("io.getstream:photoview:1.0.3") // Para visualização de fotos com zoom
    implementation("androidx.activity:activity-ktx:1.9.3") // Verifique a versão mais recente
    implementation("androidx.core:core-ktx:1.13.1") // Verifique a versão mais recente
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Já listado acima como libs.constraintlayout
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.zxing:core:3.5.3") // Para geração/leitura de códigos de barras
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") // Para scanner de código de barras embutido

    // ViewPager2 (Se ainda for usado em alguma parte do app)
    implementation("androidx.viewpager2:viewpager2:1.1.0") // Verifique a versão mais recente

    // Glide (REMOVA OU COMENTE AS DUAS LINHAS ABAIXO SE NÃO ESTIVER MAIS USANDO GLIDE EM NENHUM OUTRO LUGAR)
    // implementation("com.github.bumptech.glide:glide:4.12.0") // Considere atualizar para uma versão mais recente se for manter
    // annotationProcessor("com.github.bumptech.glide:compiler:4.12.0") // Considere atualizar para uma versão mais recente se for manter

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}