export LEIN_ROOT=yes

# update networker
cd ../networker && git pull && ./gradlew uploadArchives && cd -

# build new server version
lein do clean, test, uberjar || exit -1

echo REPLACE UBERJAR
mv ./target/*standalone.jar . || exit -1

echo MKDIR LOGS
mkdir -p ./logs || exit -1
#rename ./logs/log file to ./logs/[date]

# restart
echo PKILL OLD SERVER PROCESS
pkill -f sneer.server-0.1.0-SNAPSHOT-standalone.jar
echo START NEW SERVER PROCESS
java -jar ./sneer.server-0.1.0-SNAPSHOT-standalone.jar > ./logs/log&
echo DONE

