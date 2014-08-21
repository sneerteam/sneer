#Delete all untracked files and directories (-d), even ignored ones (-x).
git clean -x -d --force --quiet

#Revert all modified files.
git reset --hard

git pull --rebase

./gradlew install

./gradlew eclipse

cd android && ./gradlew jarNodeps eclipse && cd -

if [ -d "../rockpaperscissors" ]; then
	rm -f ../rockpaperscissors/libs/sneer-android-api-nodeps-*.jar
	cp -f android/android-api/build/libs/sneer-android-api-nodeps-0.1.2.jar ../rockpaperscissors/libs/
fi
