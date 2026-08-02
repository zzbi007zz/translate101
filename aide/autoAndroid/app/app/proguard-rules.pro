# Room entities and DAOs
-keep class com.example.zaloauto.data.db.** { *; }

# kotlinx.serialization for Navigation routes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.zaloauto.**$$serializer { *; }
-keepclassmembers class com.example.zaloauto.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.zaloauto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# AccessibilityService
-keep class com.example.zaloauto.service.accessibility.ZaloAutomationService { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
