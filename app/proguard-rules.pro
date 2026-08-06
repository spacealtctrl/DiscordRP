-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class net.spacealtctrl.discordrp.**$$serializer { *; }
-keepclassmembers class net.spacealtctrl.discordrp.** { *** Companion; }
-keepclasseswithmembers class net.spacealtctrl.discordrp.** { kotlinx.serialization.KSerializer serializer(...); }

-keep class net.spacealtctrl.discordrp.gateway.** { <fields>; }
-keep class net.spacealtctrl.discordrp.discord.** { <fields>; }

-keepclassmembers class io.ktor.http.** { *; }
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn java.lang.invoke.StringConcatFactory

-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
