#!/bin/bash -v

./reset-environment.sh || exit -1

./gradlew :core:publish || exit -1

#Reset environment SKUMMET successful
