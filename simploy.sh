# Show last commit message
git log -1

source /root/.android-sdk-installer/env
# Install necessary project dependencies to local maven cache
./gradlew --configure-on-demand :core:install :sneer-java-api:install :crypto:install || exit -1

cd server && ./deploy.sh && cd - || exit -1

