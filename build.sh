#!/bin/bash
# Build the APK without Gradle / Android Studio.
# Ubuntu/Debian:  sudo apt install aapt dalvik-exchange apksigner zipalign android-sdk-platform-23 openjdk-17-jdk
set -e
export LC_ALL=C.UTF-8
SDK=${ANDROID_SDK:-/usr/lib/android-sdk}
JAR=${ANDROID_JAR:-$SDK/platforms/android-23/android.jar}
BT=$(ls -d $SDK/build-tools/*/ | head -1)
B=build
rm -rf $B && mkdir -p $B/gen $B/obj

echo "== engine tests"
javac -d $B/test -sourcepath app/src app/src/tw/thomas/calc/CalcEngine.java test/EngineTest.java
java -cp $B/test EngineTest test/cases.txt

echo "== compile"
aapt package -f -m -J $B/gen -M app/AndroidManifest.xml -S app/res -A app/assets -I $JAR -F $B/unsigned.apk
javac -Xlint:-options --release 8 -classpath $JAR -d $B/obj $(find app/src $B/gen -name '*.java')
$BT/dx --dex --output=$B/classes.dex $B/obj
(cd $B && aapt add unsigned.apk classes.dex >/dev/null)
zipalign -f -p 4 $B/unsigned.apk $B/aligned.apk

echo "== sign"
KS=${KEYSTORE:-calc.keystore}; PASS=${KEYSTORE_PASS:-calcpass}
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -alias calc -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS" -dname "CN=Calculator, C=TW" >/dev/null 2>&1
apksigner sign --ks "$KS" --ks-pass pass:"$PASS" --key-pass pass:"$PASS" --out $B/Calculator.apk $B/aligned.apk
apksigner verify $B/Calculator.apk && echo "OK -> $B/Calculator.apk"
