gradle-jooq-plugin
==================

# Overview
[Gradle](http://www.gradle.org) plugin that integrates [jOOQ](http://www.jooq.org). For each jOOQ configuration declared
in the build, the plugin adds a task to generate the jOOQ Java sources from a given database schema and includes the
generated sources in the specified source set. Multiple configurations are supported. The code generation tasks fully
participate in the Gradle uptodate checks. The plugin can be applied on both Java projects and Android projects.

You can find out more details about the actual jOOQ source code generation in the
[jOOQ documentation](http://www.jooq.org/doc/latest/manual/code-generation).

The plugin is hosted on the [Gradle Plugin portal](https://plugins.gradle.org/plugin/nu.studer.jooq).

# Applying the plugin

You can apply the plugin using the `plugins` DSL

```groovy
plugins {
    id 'nu.studer.jooq' version '2.0.2'
}
```

Or using the `buildscript` block

```groovy
buildscript {
  repositories {
    maven {
      url 'https://plugins.gradle.org/m2/'
    }
  }
  dependencies {
    classpath 'nu.studer:gradle-jooq-plugin:2.0.2'
  }
}

apply plugin: 'nu.studer.jooq'
```

# Defining your database drivers

Depending on which database you are connecting to, you need to put the corresponding driver on the generator's classpath.

```groovy
dependencies {
    jooqRuntime 'postgresql:postgresql:9.1-901.jdbc4'
}
```

# Specifying the jOOQ version and edition

This plugin supports existing and future jOOQ versions. It also supports the different editions like open source, pro, and trial.

```groovy
jooq {
  version = '3.8.5' // the default (can be omitted)
  edition = 'OSS'   // the default (can be omitted), other allowed values are PRO, PRO_JAVA_6, and TRIAL
}
```

The plugin ensures that all your dependencies use the version and edition
specified in the `jooq` configuration. So when you declare a compile dependency
on jOOQ, you can omit the version:

```groovy
dependencies {
  compile 'org.jooq:jooq'
}
```

# Tasks
For each jOOQ configuration declared in the build, the plugin adds a new `generate[ConfigurationName]JooqSchemaSource`
task to your project. Each task generates the jOOQ Java sources from the configured database schema and includes these
sources in the specified source set. For example, a jOOQ configuration named `sample` will cause the plugin to add a
new code generation task `generateSampleJooqSchemaSource` to the project.

```console
gradle generateSampleJooqSchemaSource
```

The code generation tasks are automatically configured as dependencies of the corresponding source compilation tasks
provided by the `JavaBasePlugin` plugin. Hence, running a build that eventually needs to compile sources will first 
trigger the required jOOQ code generation tasks.

To see the log output of the jOOQ code generation tool, run the Gradle build with log level `info`:

```console
gradle build -i
```

# Configuration

The example below shows a jOOQ configuration that creates the jOOQ Java sources from a PostgreSQL database schema and
includes them in the `main` source set.

By default, the generated sources are written to `build/generated-src/jooq/<configurationName>`. The
output directory can be configured by explicitly setting the `directory` attribute of the `target` configuration.

See the [jOOQ XSD](http://www.jooq.org/xsd/jooq-codegen-3.3.0.xsd) for the full set of configuration options.

```groovy
jooq {
   version = '3.8.5'
   edition = 'OSS'
   sample(sourceSets.main) {
       jdbc {
           driver = 'org.postgresql.Driver'
           url = 'jdbc:postgresql://localhost:5432/sample'
           user = 'some_user'
           password = 'secret'
           schema = 'public'
           properties {
               property {
                   key = 'ssl'
                   value = 'true'
               }
           }
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

# Changelog
+ 2.0.2 - Configuration of call-backs for code generation java execution process
+ 2.0.1 - Bug fixes 
+ 2.0.0 - jOOQ version used for the code generation is independent from the jOOQ version used by the gradle-jooq plugin
+ 1.0.6 - Upgrade to jOOQ 3.6.2

# Acknowledgements

+ [oehme](https://github.com/oehme) (pr)
+ [jamespedwards42](https://github.com/jamespedwards42) (idea)
+ [dubacher](https://github.com/dubacher) (patch)
+ [lukaseder](https://github.com/lukaseder) (patch)

# License

This plugin is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

(c) by Etienne Studer
