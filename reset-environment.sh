echo "-------------------> Resetting workspace and pulling changes"

md5_before_pull=`md5sum $0`

#Delete all untracked files and directories (-d), even ignored ones (-x).
git clean -x -d --force --quiet

#Revert all modified files.
git reset --hard

git pull --rebase

md5_after_pull=`md5sum $0`

if [ "$md5_before_pull" != "$md5_after_pull" ] then
	echo "-------------------> This script changed, rerunning it"
	trap "$0" 0
	exit 0
fi

echo "-------------------> Preparing workspace"

./gradlew install eclipse

cd android && ./gradlew jarNodeps eclipse && cd - || echo "Error preparing android projects, aborting." && exit -1

if [ -d "../rockpaperscissors" ]; then
	rm -f ../rockpaperscissors/libs/sneer-android-api-nodeps-*.jar
	cp -f android/android-api/build/libs/sneer-android-api-nodeps-0.1.2.jar ../rockpaperscissors/libs/
fi
