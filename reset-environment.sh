#!/bin/bash
echo "-------------------> Resetting workspace and pulling changes"

hash_before_pull=$(git hash-object "$0")

git pull --rebase || exit -1

hash_after_pull=$(git hash-object "$0")

#Delete all untracked files and directories (-d), even ignored ones (-x).
git clean -x -d --force --quiet

if [ "$hash_before_pull" != "$hash_after_pull" ]; then
	echo "-------------------> This script changed, rerunning it"
	trap '$0' 0
	exit 0
fi

echo "-------------------> Preparing the workspace"

rm ~/.m2/repository/me/sneer/ -rf

./gradlew clean check install jarNodeps || exit -1

if [ -d "../lizardspock" ]; then
	echo COPYING NODEPS JAR...
	rm -f ../lizardspock/libs/sneer-android-api-nodeps-*.jar || exit -1
	cp -f android/android-api/build/libs/sneer-android-api-nodeps-*.jar ../lizardspock/libs/ || exit -1
fi

echo "Reset environment sucessful"
