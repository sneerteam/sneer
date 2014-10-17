cd android &&

#Remove android.main.tests stuff that is not working :(
cp settings.gradle settings.gradle.bak &&
grep -Ev "test" settings.gradle > temp &&
mv temp settings.gradle &&

./gradlew installDebug &&

#Recover original with android.main.tests stuff.
mv settings.gradle.bak settings.gradle
