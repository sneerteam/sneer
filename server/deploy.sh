# Allow running lein as root
export LEIN_ROOT=yes

# Build new server version
rm -rf /root/.m2/repository/me/sneer/
git clean -d -x --force --quiet
lein do clean, midje, uberjar || exit -1

mkdir -p /root/sneer-live      || exit -1
mkdir -p /root/sneer-live/logs || exit -1

echo REPLACE UBERJAR
mv target/uberjar/*standalone.jar /root/sneer-live/. || exit -1

echo PKILL OLD SERVER PROCESS
pkill -f sneer.server-0.1.0-SNAPSHOT-standalone.jar

./start.sh

echo DONE
