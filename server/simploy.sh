# Allow running lein as root
export LEIN_ROOT=yes

# Build new server version
lein do clean, midje, uberjar || exit -1

mkdir -p /root/sneer-live      || exit -1
mkdir -p /root/sneer-live/logs || exit -1

echo REPLACE UBERJAR
mv target/uberjar/*standalone.jar /root/sneer-live/. || exit -1

echo PKILL OLD SERVER PROCESS
pkill -f sneer.server-0.1.0-SNAPSHOT-standalone.jar

./start-server.sh

echo DONE
