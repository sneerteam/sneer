#!/bin/bash -v

./reset-environment-for-sims.sh

cd core       && ../gradlew clean check install && cd .. || exit -1
cd server     && ../gradlew clean check install && cd .. || exit -1
cd core.tests && ../gradlew clean check install && cd .. || exit -1

#Reset environment successful
