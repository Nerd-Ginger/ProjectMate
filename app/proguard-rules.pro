# kotlinx.serialization keeps its generated serializers via @Serializable;
# R8 needs the companion Companion.serializer() entry points preserved.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room 3 generated implementations
-keep class * extends androidx.room3.RoomDatabase { <init>(); }
