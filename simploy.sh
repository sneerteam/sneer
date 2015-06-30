# Show last commit message
git log -1

source /root/.android-sdk-installer/env

# Install necessary project dependencies to local maven cache
rm -rf /root/.m2/repository/me/sneer/
git clean -d -x --force --quiet
./gradlew --configure-on-demand :core:install :sneer-java-api:install :crypto:install || exit -1

cd server && ./deploy.sh && cd - || exit -1

