echo ---- Linux Package Upgrades
aptitude -y update
aptitude -y safe-upgrade

echo ---- Timezone
echo "America/Sao_Paulo" > /etc/timezone
dpkg-reconfigure -f noninteractive tzdata

mkdir /root/webserver
apt-get -y install webfs
sed -i "s#web_root=\"/srv/ftp\"#web_root=\"/root/webserver\"#" /etc/webfsd.conf
sed -i "s#web_port=\"\"#web_port=\"80\"#" /etc/webfsd.conf
sed -i "s#web_user=\"www-data\"#web_user=\"root\"#" /etc/webfsd.conf
sed -i "s#web_group=\"www-data\"#web_group=\"root\"#" /etc/webfsd.conf
service webfs restart

apt-get -y install git

apt-get -y install openjdk-7-jdk

wget https://raw.github.com/technomancy/leiningen/stable/bin/lein
chmod a+x lein
mv lein /usr/bin

cd /root
git clone git://github.com/klauswuestefeld/simploy.git
git clone git://github.com/sneerteam/sneer.git

cp /root/sneer/server/install/sneerServerBoot.sh /etc/init.d/
chmod +x /etc/init.d/sneerServerBoot.sh
update-rc.d sneerServerBoot.sh defaults 80


echo ---- Android SDK in /root/.android-sdk-installer/env
apt-get -y install -qq libstdc++6:i386 lib32z1
apt-get -y install expect
curl -3L https://raw.github.com/embarkmobile/android-sdk-installer/version-2/android-sdk-installer | bash /dev/stdin --install=build-tools-19.0.3,android-19,sysimg-19

cat << EOFEOF
---- SNAPI maven publishing

SNAPI build needs nexus credentials and a private key for signing up and upload
the artifacts to maven central.

Do this by creating a /root/snapi/gradle.properties with the following content:

## properties begin
signing.keyId=<key ID>
signing.password=<password>
signing.secretKeyRingFile=<key ring file>

nexusUsername=KlausWuestefeld
nexusPassword=<password to nexus/sonatype>
## properties end

The key you use to sign the package must be registered at
pool.sks-keyservers.net so nexus server can verify it.

Have a good day.
EOFEOF
