#!/bin/bash
# Screenshots of the -Pdemo -Pshots build in one language, opened straight on each screen by intent extras.
# Usage: tools/capture.sh en|ru   (ANDROID_SERIAL picks the phone; the 13 Pro by default)
set -e
export ANDROID_SERIAL=${ANDROID_SERIAL:-a78f414d} JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
cd "$(dirname "$0")/.."
L=$1; P=app.questling.demo; OUT=docs/screenshots/$L; mkdir -p $OUT
./gradlew assembleDebug -Pdemo -Pshots -Pdemolang=$L -q 2>&1 | grep -E "^e: |FAILED" && exit 1
adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
adb shell pm clear $P >/dev/null
adb shell am start -n $P/app.questling.MainActivity >/dev/null; sleep 240   # the seed, then the covers from Wikipedia
shot() { [ "$(adb shell dumpsys telephony.registry | grep -m1 mCallState | tr -dc 0-9)" = "0" ] || { echo "call"; exit 1; }
  adb shell am start -S -n $P/app.questling.MainActivity $2 >/dev/null; sleep ${3:-6}
  f=$(adb shell dumpsys window | grep -m1 mCurrentFocus); echo "$f" | grep -q "$P" || { echo "focus lost: $f"; exit 1; }
  adb exec-out screencap -p > $OUT/$1.png; }
shot 1-home "--ei tab 0" 8
shot 2-journal "--ei tab 1"
shot 3-trophies "--ei tab 2"
shot 4-pets "--ei tab 3"
shot 5-games "--ei tab 1 --es open games"
shot 6-game "--ei tab 1 --es open game:3"
adb shell am force-stop $P
echo done $L
