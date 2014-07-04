gradle-jooq-plugin
==================

# Overview
[Gradle](http://www.gradle.org) plugin that integrates [jOOQ](http://www.jooq.org). For each jOOQ configuration declared 
in the build, the plugin adds a task to generate the jOOQ Java sources from a given database schema, and includes the
generated sources in the specified source set. Multiple configurations are supported. The code generation tasks fully 
participate in the Gradle uptodate checks.

You can find out more details about the actual jOOQ source code generation in the
[jOOQ documentation](http://www.jooq.org/doc/3.3/manual/code-generation).

# Plugin

##General
The jOOQ plugin automatically applies the Java plugin. Thus, there is no need to explicitly apply the Java plugin in
your build script when using the jOOQ plugin.

Depending on the type of database that is accessed to derive the jOOQ Java sources, the corresponding driver must
be made available on the plugin classpath.

The jOOQ plugin is hosted at [JCenter](https://bintray.com/bintray/jcenter).

## Gradle 1.x and 2.0
To use the jOOQ plugin with versions of Gradle older than 2.1, apply the plugin in your `build.gradle` script:

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'nu.studer:gradle-jooq-plugin:1.0.3'
        classpath 'postgresql:postgresql:9.1-901.jdbc4' // database-specific JDBC driver
    }
}
apply plugin: 'nu.studer.jooq'
```

## Gradle 2.1 and higher
As of Gradle 2.1, declare the plugin in your `build.gradle` script:

```groovy
plugins {
    id 'nu.studer.jooq' version '1.0.3'
}
```

# Tasks
For each jOOQ configuration declared in the build, the plugin adds a new `generate[ConfigurationName]JooqSchemaSource` 
task to your project. Each task generates the jOOQ Java sources from the configured database schema and includes these
sources in the specified source set. For example, a configuration named `sample` will add a code generation task 
`generateSampleJooqSchemaSource` to the project.

```console
gradle generateSampleJooqSchemaSource
```

The code generation tasks are automatically configured as dependencies of the corresponding source compilation tasks
of the Java plugin. Hence, running a build that eventually needs to compile sources will first trigger the required
jOOQ code generation tasks.

To see the log output of the jOOQ code generation tool, run the Gradle build with log level `info`:

```console
gradle build -i
```

# Configuration

The example below shows a jOOQ configuration that creates the jOOQ Java sources from a PostgreSQL database schema and 
includes them in the `main` source set.

By default, the generated sources are written to `build/generated-src/jooq/<sourceSet>/<configurationName>`. The 
output directory can be configured by explicitly setting the `directory` attribute of the `target` configuration.

See the [jOOQ XSD](http://www.jooq.org/xsd/jooq-codegen-3.3.0.xsd) for the full set of configuration options.

```groovy
jooq {
   sample(sourceSets.main) {
       jdbc {
           driver = 'org.postgresql.Driver'
           url = 'jdbc:postgresql://localhost:5432/sample'
           user = 'some_user'
           password = 'secret'
           schema = 'public'
           properties = []
       }
       generator {
           name = 'org.jooq.util.DefaultGenerator'
           strategy {
               name = 'org.jooq.util.DefaultGeneratorStrategy'
               // ...
           }
           database {
               name = 'org.jooq.util.postgres.PostgresDatabase'
               inputSchema = 'public'
               // ...
           }
           generate {
               relations = true
               deprecated = false
               records = true
               immutablePojos = true
               fluentSetters = true
               // ...
           }
           target {
               packageName = 'nu.studer.sample'
               // directory = ...
           }
       }
   }
}
```

# License
This plugin is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

(c) All rights reserved Etienne Studer
