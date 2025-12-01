#!/bin/bash
# This assumes "$ADB devices" lists one device.

set -eu

./gradlew assembleDebug

ADB=$HOME/Android/Sdk/platform-tools/adb
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
$ADB shell am start -n com.fghbuild.sidekick/.MainActivity
# $ADB logcat YourAppTag:D *:S
$ADB logcat | grep "com.fghbuild.sidekick"
