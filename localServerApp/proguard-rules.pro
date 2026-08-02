## Rules for NewPipeExtractor
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**

## Rules for Java-WebSocket
-keep class org.java_websocket.** { *; }
-dontwarn org.java_websocket.**

## Suppress warnings for classes missing in Android Runtime
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-dontwarn org.slf4j.**
