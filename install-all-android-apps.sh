#!/bin/bash
cd android &&

# Remove android.main.tests stuff that is not working :(
cp settings.gradle settings.gradle.bak &&
grep -Ev "test" settings.gradle > temp &&
mv temp settings.gradle &&

# Install everything \o/
./gradlew installDebug &&

# Recover original with android.main.tests stuff.
mv settings.gradle.bak settings.gradle

cd -

# Install lizardspock
# It must be a directory in ../lizardspock
cd ../lizardspock/ || exit -1
echo "-------------------> Installing lizardspock"
./gradlew installDebug
