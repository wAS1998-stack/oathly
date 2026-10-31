#!/usr/bin/env bash
# Runs on the GitHub emulator: clean status bar, install, walk through the app, collect screenshots.
set -x
adb wait-for-device
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile show -e datatype none -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c
adb shell am instrument -w -r com.oathly.app.debug.test/androidx.test.runner.AndroidJUnitRunner > instrument.txt 2>&1
mkdir -p screenshots
adb pull /sdcard/Android/data/com.oathly.app.debug/files/screenshots/. screenshots/ || true
adb logcat -d -b crash > crash.txt || true
ls -la screenshots
grep -q "OK (" instrument.txt
