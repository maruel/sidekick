#!/bin/bash
# This assumes "$ADB devices" lists one device.

set -eu

./gradlew assembleDebug

ADB=adb
if ! which adb >/dev/null; then
	ADB=$HOME/Android/Sdk/platform-tools/adb
fi

DEVICE_COUNT=$($ADB devices | grep -v "List of devices attached" | grep -v "daemon" | grep -v "^$" | wc -l)
if [ "$DEVICE_COUNT" -ne 1 ]; then
	echo "WARNING: Expected 1 device, but found $DEVICE_COUNT"
	$ADB devices
fi
ADB_DEVICE=$($ADB devices | grep -v "List of devices attached" | grep -v "daemon" | grep -v "^$" | head -n 1 | cut -f1)

$ADB -s "$ADB_DEVICE" install -r app/build/outputs/apk/debug/app-debug.apk
$ADB -s "$ADB_DEVICE" shell am start -n com.fghbuild.sidekick/.MainActivity
# $ADB -s "$ADB_DEVICE" logcat YourAppTag:D *:S
# $ADB -s "$ADB_DEVICE" logcat | grep "com.fghbuild.sidekick"
