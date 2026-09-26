# ProGuard rules for BB10 Launcher
# Services and activities are referenced from the manifest and kept by AAPT's generated rules.

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
