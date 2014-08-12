# Show last commit message
git log -1

# Install necessary project dependencies to local maven cache
./gradlew install || exit -1

cd server && ./simploy.sh && cd - || exit -1
