# Regras do ProGuard para Gson (se você usar para serialização/desserialização com Retrofit)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.TypeAdapter

# Manter classes de modelo (data classes)
# Substitua "com.example.myapplication.model" pelo pacote real dos seus modelos
-keep class com.example.myapplication.model.** { *; }
# Se você tiver outras classes de dados que precisam ser mantidas, adicione regras para elas
# Exemplo: -keep class com.example.myapplication.data.** { *; }

# Manter nomes de campos em classes de modelo se forem usados para serialização/desserialização
-keepclassmembers class com.example.myapplication.model.** {
    <fields>;
}
# Exemplo para outras classes de dados:
# -keepclassmembers class com.example.myapplication.data.** {
#    <fields>;
# }

# Para Parcelize (geralmente o plugin Kotlin cuida disso, mas como precaução)
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Para ViewBinding (geralmente não precisa de regras explícitas com plugins recentes do AGP)
# -keep class * extends androidx.viewbinding.ViewBinding
# -keepclassmembers class **/*Binding {
#    public static *** inflate(android.view.LayoutInflater);
#    public static *** bind(android.view.View);
# }

# Para Glide (a biblioteca geralmente inclui suas próprias regras ProGuard)
# -keep public class * implements com.bumptech.glide.module.GlideModule
# -keep public class * extends com.bumptech.glide.module.AppGlideModule
# -keep public enum com.bumptech.glide.load.ImageHeaderParser$ImageType
# -keep public interface com.bumptech.glide.load.resource.transcode.ResourceTranscoder
# -keepclassmembers class * {
#   @com.bumptech.glide.annotation.GlideModule <methods>;
# }

# Para ZXing (leitura de código de barras da JourneyApps)
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }

# Se você usar ML Kit Text Recognition
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text.** { *; }


# Manter Activities, Services, BroadcastReceivers, ContentProviders que são referenciados no AndroidManifest.xml
# O ProGuard geralmente faz isso automaticamente, mas adicione explicitamente se tiver problemas.
# Exemplo:
# -keep public class com.example.myapplication.MainActivity
# -keep public class * extends android.app.Activity
# -keep public class * extends android.app.Application
# -keep public class * extends android.app.Service
# -keep public class * extends android.content.BroadcastReceiver
# -keep public class * extends android.content.ContentProvider
# -keep public class * extends android.app.backup.BackupAgentHelper
# -keep public class * extends android.preference.Preference

# Manter construtores de Views personalizadas se você tiver alguma
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Manter nomes de métodos nativos
-keepclasseswithmembernames class * {
    native <methods>;
}

# Manter enumerações usadas em anotações
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Regras específicas para Retrofit e OkHttp
-dontwarn okio.**
-dontwarn retrofit2.Platform$Java8      # Para compatibilidade com Java 8 em versões mais antigas do Android
-keep interface retrofit2.Call       # Manter a interface Call
-keep class retrofit2.Response       # Manter a classe Response
-keep class retrofit2.Retrofit       # Manter a classe Retrofit
-keepattributes Exceptions

# Se você usar Kotlin Coroutines com Retrofit
-keepclassmembers class kotlinx.coroutines.android.MainDispatcherFactory {
    private static final kotlinx.coroutines.android.MainDispatcherFactory INSTANCE;
    public static final kotlinx.coroutines.android.MainCoroutineDispatcher getMain();
}
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory {
    private static final kotlinx.coroutines.internal.MainDispatcherFactory INSTANCE;
    public static final kotlinx.coroutines.MainCoroutineDispatcher getMain();
}
-keepclassmembers class kotlinx.coroutines.DefaultExecutor {
    private static final kotlinx.coroutines.DefaultExecutor INSTANCE;
    public static final kotlinx.coroutines.ExecutorCoroutineDispatcher Main;
}

# Manter nomes de classes e membros que são usados por XML (ex: custom views, data binding)
-keepnames class * extends android.view.View
-keepnames class * extends androidx.databinding.ViewDataBinding

# Se usar io.getstream.photoview
-keep class io.getstream.photoview.** { *; }