# Keep kotlinx.serialization classes
-keepclassmembers class com.korvus.pocketmiku.** {
    *;
}
-keep,includedescriptorclasses class com.korvus.pocketmiku.**$$serializer { *; }
-keepclassmembers class com.korvus.pocketmiku.** {
    *** Companion;
}
-keepclasseswithmembers class com.korvus.pocketmiku.** {
    kotlinx.serialization.KSerializer serializer(...);
}
