# ProGuard rules for BB10 Hub Launcher

# Keep Android framework classes
-keep class android.** { *; }
-keep interface android.** { *; }

# Keep Jetpack/AndroidX classes
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Keep Kotlin classes
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Keep notification service
-keep class app.lawnchair.bb10hub.HubNotificationService { *; }
-keep class app.lawnchair.bb10hub.Bb10HubActivity { *; }
-keep class app.lawnchair.bb10hub.BB10HubScreen { *; }

# Keep all model classes
-keep class app.lawnchair.bb10hub.** { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep view constructors (for inflation from XML)
-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimization flags
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# Renaming
-allowaccessmodification

# Remove unused code
-dontshrink
-dontoptimize
