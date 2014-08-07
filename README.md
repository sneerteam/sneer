Getting Started
====

- Install Open-JDK-7 and the [Eclipse-based ADT Bundle](http://developer.android.com/sdk/installing/index.html?pkg=adt)

- Clone this repository.

- Run:
```
./reset-environment.sh
```
You can run the above every time you want a clean rebuild.

- Import all projects contained in this repository into the Eclipse workspace.

(Optional) To work on the Sneer core in Clojure:
- Install the [Counterclockwise](http://code.google.com/p/counterclockwise/) Clojure plugin for Eclipse.


Preparing Workspace for App Development
====

To use the Sneer Android Client in your application you can either copy jar file that contains all sneer dependencies into your project's libs folder, or reference the android-api project in your workspace.

Using the Jar
----

The last version of the jar file can be downloaded from http://dynamic.sneer.me/dist/sneer-android-api-nodeps

If you prefer to generate the jar from the source codes directly with:
```
./gradlew jarNodeps
```

This creates the file android-api/build/libs/sneer-android-api-nodeps-X.X.X.jar. Copy it to our libs folder and you're good to go.

Using the Projects
----

By referencing the sneer source code projects directly in your workspace is specially useful if you're planning to change sneer source codes. Eclipse refactoring, call hierarchy and some of those facilities work better if you reference the project instead of the jar file.

To do so, after you import the sneer projects into the workspace and:

- right click your project then click Properties
- go to Android settings
- click Add... and select the android-api project
- still in the settings window go to Java Build Path -> Projects -> Add... and select again the android-api project
- and still in the Java Build Path -> Order and Export, make sure android-api project comes before the android dependencies in that list
