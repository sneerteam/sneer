#! /bin/bash
cd android/android.main.tests

export ANDROID_SERIAL=`adb devices | grep 'device$' | cut -f1 | head -n 1`

echo "Running tests on device $ANDROID_SERIAL"
ant clean test

echo "Finished running tests"

cd -
