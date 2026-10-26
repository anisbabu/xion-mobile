#!/usr/bin/env bash
# Boots the test emulator (headless), installs the debug APK and launches Xion.
# Usage: bash tools/emu-run.sh [portrait|landscape]
export ANDROID_SDK_ROOT='D:\Android\Sdk' ANDROID_AVD_HOME='D:\Android\avd'
A=/d/Android/Sdk/platform-tools/adb.exe
APK=/d/XionMobile/app_pojavlauncher/build/outputs/apk/debug/app_pojavlauncher-debug.apk

if [ "$($A shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; then
  (cd /d/Android/Sdk/emulator && ./emulator.exe -avd xion_phone -no-window -no-audio -no-boot-anim -gpu host -no-snapshot -memory 2048 > /d/Android/emu.log 2>&1 &)
  for i in $(seq 1 80); do
    [ "$($A shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && break
    sleep 5
  done
fi
echo "boot=$($A shell getprop sys.boot_completed | tr -d '\r')"
$A shell settings put system accelerometer_rotation 0
[ "$1" = "landscape" ] && $A shell settings put system user_rotation 1 || $A shell settings put system user_rotation 0
$A install -r -g "$APK" | tail -1
$A shell am force-stop net.xion.mobile.debug
$A shell monkey -p net.xion.mobile.debug -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
sleep 10
