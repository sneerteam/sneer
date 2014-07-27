DISTDIR=/root/webserver/dist/snapi-nodeps

. /root/.android-sdk-installer/env

echo "------ Building networker"
cd ../networker && git pull && ./gradlew uploadArchives && cd -

echo "------ Removing build dir"
rm -rf build

echo "------ Building SNAPI"
./gradlew clean uploadArchives -Pci 
./gradlew jarNodeps -Pci -PaddTimestamp 

echo "------ Publishing sneer-android-api-nodeps"
mkdir -p $DISTDIR
cp build/libs/sneer-android-api-nodeps-*.jar $DISTDIR

echo "------ Cleaning all but the last 3 jar files in $DISTDIR"
cd $DISTDIR && ls -t | tail -n+4 | xargs rm -rf && cd -  

echo DONE
