gradle-jooq-plugin
==================

# Overview
[Gradle](http://www.gradle.org) plugin that integrates [jOOQ](http://www.jooq.org).
For each source set, the plugin adds a task to generate the jOOQ Java sources from a given database schema.
The schema generation tasks fully participate in the Gradle uptodate checks.

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
Assuming the default source sets of the Java plugin, the tasks `generateJooqSchemaSource` and `generateTestJooqSchemaSource` are available.
The schema generation tasks are automatically configured as dependencies of the corresponding source compilation tasks of the Java plugin.

## Configuration

### build.gradle
The jOOQ code generation is configured per target source set.
The configuration below shows a sample configuration that will create the jOOQ Java sources and include them in the `main` source set.
See the [jOOQ XSD](http://www.jooq.org/xsd/jooq-codegen-3.3.0.xsd) for the full set of configuration options.

```groovy
jooq {
   main {
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
