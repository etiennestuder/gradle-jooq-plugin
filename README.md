gradle-jooq-plugin
==================

# Overview
[Gradle](http://www.gradle.org) plugin that integrates [jOOQ](http://www.jooq.org).
The plugin adds a task for each source set to generate the jOOQ sources from a given database schema.
The tasks fully participate in the Gradle uptodate checks.

# Usage
To use the plugin, configure your `build.gradle` script and add the plugin:
```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'nu.studer:gradle-jooq-plugin:1.0'

    }
}
apply plugin: 'nu.studer.jooq'
```

# Tasks
The plugin extends the Java plugin and, for each source set, adds a new `generate[SourceSet]JooqSchemaSource` task to your project.
Each task generates the jOOQ Java sources from the configured database schema and includes them in the corresponding source set.
Using the default source sets of the Java plugin, the tasks `generateJooqSchemaSource` and `generateTestJooqSchemaSource` are available.


## Configuration

### build.gradle
```groovy
jooq {
   main {
       jdbc {
           driver = 'org.postgresql.Driver'
           url = 'jdbc:postgresql://localhost:5432/sample'
           user = 'some_user'
           password = 'secret'
           schema = 'public'
       }
       generator {
           name = 'org.jooq.util.DefaultGenerator'
           database {
               name = 'org.jooq.util.postgres.PostgresDatabase'
               inputSchema = 'public'
           }
           generate {
               deprecated = false
               records = true
               immutablePojos = true
               fluentSetters = true
           }
           target {
               packageName = 'nu.studer.sample'
           }
       }
   }
}
```

See the [jOOQ XSD](http://www.jooq.org/xsd/jooq-codegen-3.3.0.xsd) for the full set of configuration options.

# License
This plugin is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

(c) All rights reserved Etienne Studer
