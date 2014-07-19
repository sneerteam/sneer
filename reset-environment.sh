git clean -x -d --force --quiet
git reset --hard
git pull --rebase

#Is "gradlew" necessary before "gradlew eclipse"?
./gradlew
./gradlew eclipse
