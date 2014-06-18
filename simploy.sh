. /root/.android-sdk-installer/env

cd ../networker && git pull && ./gradlew uploadArchives && cd -

rm -rf build
./gradlew uploadArchives -Pci 

echo DONE
