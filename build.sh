#!/bin/bash
# Build the APK without Gradle / Android Studio.
# Needs JDK 17+ and, from the Android SDK: build-tools 35 (aapt2, d8, zipalign, apksigner) and platform 34.
# (d8 from build-tools 34 crashes on classes built by JDK 21; 35.0.1 is fine.)
#   ANDROID_SDK  default /opt/asdk   (expects build-tools under bt35/*/ and the platform under p34/*/,
#                                      or set BUILD_TOOLS and ANDROID_JAR directly)
set -euo pipefail   # any failing command, including one inside a pipeline, stops the build
export LC_ALL=C.UTF-8
SDK=${ANDROID_SDK:-/opt/asdk}
BT=${BUILD_TOOLS:-$(ls -d $SDK/bt35/*/ 2>/dev/null | head -1 || true)}
JAR=${ANDROID_JAR:-$(ls $SDK/p34/*/android.jar 2>/dev/null | head -1 || true)}
[ -x "$BT/aapt2" ] && [ -f "$JAR" ] || { echo "Android build-tools 35 / platform 34 not found (set BUILD_TOOLS and ANDROID_JAR)"; exit 1; }
B=build
rm -rf $B && mkdir -p $B/gen $B/obj $B/dex

echo "== engine tests"
javac -d $B/test -sourcepath app/src app/src/tw/thomas/calc/CalcEngine.java test/EngineTest.java
java -cp $B/test EngineTest test/cases.txt
javac -d $B/test -cp $B/test test/EngineFuzz.java
java -cp $B/test EngineFuzz 20000

echo "== resources"
$BT/aapt2 compile --dir app/res -o $B/res.zip
$BT/aapt2 link -o $B/unsigned.apk -I "$JAR" --manifest app/AndroidManifest.xml -A app/assets --java $B/gen $B/res.zip

echo "== compile"
# a compile error now stops the build (previously "| grep ... || true" hid javac's exit status)
javac -nowarn -encoding UTF-8 --release 8 -Xlint:-options -classpath "$JAR" -d $B/obj $(find app/src $B/gen -name '*.java')
(cd $B/obj && jar cf ../classes.jar .)
$BT/d8 --release --min-api 23 --lib "$JAR" --output $B/dex $B/classes.jar
python3 -c "import zipfile,sys; z=zipfile.ZipFile(sys.argv[1],'a'); z.write(sys.argv[2],'classes.dex'); z.close()" $B/unsigned.apk $B/dex/classes.dex
$BT/zipalign -f -p 4 $B/unsigned.apk $B/aligned.apk

echo "== sign"
KS=${KEYSTORE:-calc.keystore}; PASS=${KEYSTORE_PASS:-calcpass}
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -alias calc -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS" -dname "CN=Calculator, C=TW" >/dev/null 2>&1
$BT/apksigner sign --ks "$KS" --ks-pass pass:"$PASS" --key-pass pass:"$PASS" --out $B/Calculator.apk $B/aligned.apk
$BT/apksigner verify $B/Calculator.apk && echo "OK -> $B/Calculator.apk"
