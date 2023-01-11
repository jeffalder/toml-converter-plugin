# toml-converter-plugin

A Gradle plugin that will attempt convert your project to use `libs.versions.toml`.

Assumptions:
1. You don't have `libs.versions.toml` configured, or the version catalog plugin.
2. You are using Gradle 7.4 or later.
3. You are building with Java 11 or later.

## How to apply

Add this to your `build.gradle` in the root project:
```groovy
plugins {
    id('io.github.jeffalder.tomlconverter') version '1.0.0'
}
```

Then run this task:
```shell
./gradlew writeConversionScript
```

Then run the output script:
```shell
build/tomlConverter/convert.sh
```

**Note** that this is not a plugin you would keep long-term. Add it to do the conversion, and then remove it.

## How it works

1. Pulls all dependencies and constraints from all your `build.gradle` files in all the projects.
2. Writes the TOML file and generate keys for all your libraries.
3. Writes temporary `build.gradle` or `build.gradle.kts` files, using the generated keys.
4. Writes a UNIX shell script that will copy all these files to the correct places.

## Things you should know

**Some keys may have random suffixes.** The goal of this plugin is to produce files and IDs that _work_ on the first try. Due to some oddities in the way Gradle generates code from TOML keys, automatic key selection has to add some random suffixes to disambiguate the keys. **You should** review any suffixed keys after conversion to see if you can make more human-readable ones.

**The build.gradle replacement is not perfect.** I'm doing my best, but I'm not going to work much harder than a regex. **You should** carefully review the resulting `build.gradle` files and the dependency trees both before and after the build.

**Not all dependency formats will get replaced.** The plugin uses gradle-calculated dependency GVAC coordinates, but that might not easily match the dependency format. Some examples include:
* the map format, such as `group: 'org.junit.jupiter', name: 'junit-jupiter-api'`
* anything that string-interpolates within the group or name, such as `"org.apache.kafka:kafka_${scala.version}:3.3.1"`

Note that TOML keys _will_ be created, they just won't be replaced. **You should** check for any required manual replacements. 
