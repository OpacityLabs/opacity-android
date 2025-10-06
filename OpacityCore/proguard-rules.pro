# Keep all public API classes and methods - this is your main library interface
-keep public class com.opacitylabs.opacitycore.** { 
    public *; 
}

# Keep the main OpacityCore object and all its methods (including native methods)
-keep class com.opacitylabs.opacitycore.OpacityCore {
    <fields>;
    <methods>;
    native <methods>;
}

# Keep data classes and response models
-keep class com.opacitylabs.opacitycore.OpacityResponse { *; }
-keep class com.opacitylabs.opacitycore.OpacityError { *; }

# Keep the InAppBrowserActivity as it's referenced by string name
-keep class com.opacitylabs.opacitycore.InAppBrowserActivity { *; }

# Keep utility classes
-keep class com.opacitylabs.opacitycore.JsonUtils { *; }
-keep class com.opacitylabs.opacitycore.JsonToAnyConverter { *; }
-keep class com.opacitylabs.opacitycore.JsonToAnyConverter$Companion { *; }
-keep class com.opacitylabs.opacitycore.CryptoManager { *; }
-keep class com.opacitylabs.opacitycore.CookieResultReceiver { *; }

# Keep all enums and their methods
-keepclassmembers enum com.opacitylabs.opacitycore.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
}

# Keep companion objects (especially for OpacityCore object)
-keep class com.opacitylabs.opacitycore.**$Companion { *; }

# Keep Kotlin serialization support
-keep @kotlinx.serialization.Serializable class com.opacitylabs.opacitycore.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-dontnote kotlinx.serialization.SerializationKt

# Keep Kotlin metadata for reflection
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeVisibleTypeAnnotations

# Keep all constructors
-keepclassmembers class com.opacitylabs.opacitycore.** {
    <init>(...);
}

# Keep methods that might be called via reflection or JNI
-keepclassmembers class com.opacitylabs.opacitycore.** {
    public <methods>;
    private <methods>;
}

# Keep native methods (important for JNI)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep classes that extend Android components
-keep class * extends android.app.Activity
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.os.Parcelable

# Keep GeckoView related classes (since you use Mozilla GeckoView)
-keep class org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.geckoview.**

# Keep security crypto classes
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Keep JSON handling classes
-keep class org.json.** { *; }
-dontwarn org.json.**

# Keep coroutines support
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }

# Keep Kotlin standard library
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile