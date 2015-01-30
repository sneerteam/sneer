Getting Started
====

- Install [Oracle JDK 7](http://www.oracle.com/technetwork/pt/java/javase/downloads/index.html) and the [Android Studio](http://developer.android.com/sdk/installing/index.html?pkg=studio)

- Clone this repository.

- Run:
```
./reset-environment.sh
```
You can run the above every time you want a clean rebuild.

- Import all projects contained in this repository into Android Studio:
   [Import Non-Android Studio project] and point it to the build.gradle inside the android/ directory.

- (Optional) To work on the Sneer core in Clojure:
Install the [Cursive Clojure](https://cursiveclojure.com/) plugin for IntelliJ (works fine with Android Studio as well).


Preparing Workspace for App Development
====

Two options:

1) Using the Sneer Android API Jar
----

The last version of the jar file can be downloaded from http://dynamic.sneer.me/dist/sneer-android-api-nodeps

If you prefer to generate the jar from the source codes, run in this repository:
```
./gradlew jarNodeps
```

This will create the file android-api/build/libs/sneer-android-api-nodeps-X.X.X.jar.

Place the jar file in your app's libs folder and you're good to go.

2) Referencing Sneer Code Projects
----

Another option is referencing the Sneer projects directly in your workspace. This is specially useful if you're planning to edit Sneer code. Android Studio refactoring, call hierarchy and some of those facilities work better if you reference the project instead of the jar file.

To do so, add the android-api project to your build.gradle file like this:
```groovy
  WIP
```
