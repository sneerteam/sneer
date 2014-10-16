cd android &&
cp settings.gradle settings.gradle.bak &&
grep -Ev "test" settings.gradle > temp &&
mv temp settings.gradle &&
./gradlew installDebug &&
mv settings.gradle.bak settings.gradle
