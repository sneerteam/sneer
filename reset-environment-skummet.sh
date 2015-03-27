#!/bin/bash -v

./reset-environment.sh || exit -1

./gradlew :core:uploadSkummet || exit -1

#Reset environment sucessful
