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

Another option is referencing the Sneer projects directly in your workspace. This is specially useful if you're planning to edit Sneer code. Eclipse refactoring, call hierarchy and some of those facilities work better if you reference the project instead of the jar file.

To do so, import the Sneer projects into the workspace and:

- Open your app project's properties
- Go to Android Settings > Add...
- Select the android-api project
Still in the properties window:
- Java Build Path > Projects > Add...
- Select the android-api project again
Still in the Java Build Path:
- Order and Export: make sure Sneer's android-api project comes before "Android Dependencies" in that list
