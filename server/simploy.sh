# Allow running lein as root
export LEIN_ROOT=yes

# Update networker
cd ../networker && git pull && ./gradlew uploadArchives && cd -

# Build new server version
lein do clean, test, uberjar || exit -1

echo REPLACE UBERJAR
mv target/*standalone.jar . || exit -1

echo MKDIR LOGS
mkdir -p logs || exit -1

echo PKILL OLD SERVER PROCESS
pkill -f sneer.server-0.1.0-SNAPSHOT-standalone.jar

# Append date to previous log filename
mv logs/log "logs/log-`date +%Y-%m-%d_%H-%M`"

echo START NEW SERVER PROCESS
java -jar ./sneer.server-0.1.0-SNAPSHOT-standalone.jar > logs/log&
echo DONE
