#!/bin/bash -v

git pull --rebase || exit -1

#Delete all untracked files and directories (-d), even ignored ones (-x).
git clean -d -x --force --quiet

rm -rf ~/.m2/repository/me/sneer/

cd java-api       && ../gradlew clean check install && cd .. || exit -1
cd java-api.tests && ../gradlew clean check         && cd .. || exit -1
cd crypto         && ../gradlew clean check install && cd .. || exit -1
cd core           && ../gradlew clean               && cd .. || exit -1

cd android        &&  ./gradlew clean               && cd .. || exit -1

#Reset environment for Sims successful.
