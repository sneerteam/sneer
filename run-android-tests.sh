#! /bin/bash
cd android/android.main.tests

devices=`adb devices | grep 'device$' | cut -f1`
pids=""

for device in $devices
do
    echo "Running tests on device $device"
    adb -s $device shell am instrument -w sneer.android.test/android.test.InstrumentationTestRunner
done

echo "Finished running tests"

cd -
wait
