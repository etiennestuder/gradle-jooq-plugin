gradle-jooq-plugin
==================

> The work on this software project is in no way associated with my employer nor with the role I'm having at my employer. Any requests for changes will be decided upon exclusively by myself based on my personal preferences. I maintain this project as much or as little as my spare time permits.

# Overview
[Gradle](http://www.gradle.org) plugin that integrates the jOOQ code generation tool.

For each named jOOQ configuration declared in the build, the plugin adds a task to generate the jOOQ Java sources from the specified database schema and includes the
generated Java sources in the matching source set, if existing. The code generation tasks participate
in [task configuration avoidance](https://docs.gradle.org/current/userguide/task_configuration_avoidance.html),
in [build configuration caching](https://docs.gradle.org/nightly/userguide/configuration_cache.html),
in [incremental builds](https://docs.gradle.org/current/userguide/more_about_tasks.html#sec:up_to_date_checks),
and in [task output caching](https://docs.gradle.org/current/userguide/build_cache.html). The plugin can be applied on both Java projects and Android projects.

You can find more details about the actual jOOQ source code generation in the [jOOQ documentation](http://www.jooq.org/doc/latest/manual/code-generation).

The jOOQ plugin is hosted at [Bintray's JCenter](https://bintray.com/etienne/gradle-plugins/gradle-jooq-plugin), also available from
the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/nu.studer.jooq).

## Build scan

Recent build scan: https://gradle.com/s/bkc4davu2dvu4

Find out more about build scans for Gradle and Maven at https://scans.gradle.com.

# Functionality

The following functionality is provided by the jOOQ plugin:

 * Generate Java sources from a given database schema
 * Add the generated Java sources to the name-matching source set, if existing
 * Wire task dependencies such that the Java sources are generated before the Java compile task of the name-matching source set compiles them, if existing
 * Provide a configuration option to suppress automatic task wiring between the Java compile task and the jOOQ source generation task

The following Gradle configuration changes are contributed by the jOOQ plugin:

 * Add the `org.jooq:jooq-codegen` dependency needed to execute the jOOQ code generation tool to the new `jooqGenerate` configuration
 * Add the `org.jooq:jooq` dependency to the name-matching `implementation` configuration to successfully compile the Java sources generated from the database schema
 * Use the customizable jOOQ version across all resolved `org.jooq*:jooq*` dependencies

The following Gradle features are supported by the jOOQ plugin:

 * `JooqGenerate` task instances participate in task configuration avoidance
 * `JooqGenerate` task instances participate in configuration caching
 * `JooqGenerate` task instances participate in incremental builds
 * `JooqGenerate` task instances participate in task output caching (if the task gets explicitly marked as cacheable)

# Compatibility

| Plugin version | Compatible Gradle versions | Support for Gradle Kotlin DSL | Support for Gradle Configuration Cache |
| -------------- |--------------------------- | ----------------------------  | -------------------------------------- |
| 5.0+           | 6.1+                       | Yes                           | Yes |
| 4.0            | 5.0+, 6.0+                 | No                            | No |

# Configuration

## Applying the plugin

Apply the `nu.studer.jooq` plugin to your Gradle project.

### Gradle Groovy DSL

```groovy
plugins {
    id 'nu.studer.jooq' version '5.0.1'
}
```

### Gradle Kotlin DSL

```kotlin
plugins {
    id("nu.studer.jooq") version "5.0.1"
}
```

## Adding the database driver

Add the database driver of the database that the jOOQ code generation tool will introspect to the `jooqGenerator` configuration. This ensures that the database driver
is on the classpath when the jOOQ code generation tool is executed. Optionally, you can add additional dependencies that are required to run the jOOQ code generation tool.

### Gradle Groovy DSL

```groovy
dependencies {
    jooqGenerator 'org.postgresql:postgresql:42.2.14'
}
```

### Gradle Kotlin DSL

```kotlin
dependencies {
    jooqGenerator("org.postgresql:postgresql:42.2.14")
}
```

## Specifying the jOOQ version and edition

Specify the version and [edition](https://github.com/etiennestuder/gradle-jooq-plugin/blob/master/src/main/groovy/nu/studer/gradle/jooq/JooqEdition.java) that should be
applied to all jOOQ dependencies that are declared in your project either explicitly or pulled in transitively.

Note that the `org.jooq:jooq` dependency of the specified version and edition is automatically added to the `implementation` configuration of the source set that matches the
name of the declared jOOQ configuration.

### Gradle Groovy DSL

```groovy
jooq {
  version = '3.13.4'  // the default (can be omitted)
  edition = nu.studer.gradle.jooq.JooqEdition.OSS  // the default (can be omitted)
}
```

### Gradle Kotlin DSL

```kotlin
jooq {
  version.set("3.13.4")  // the default (can be omitted)
  edition.set(nu.studer.gradle.jooq.JooqEdition.OSS)  // the default (can be omitted)
}
```

## Enforcing the jOOQ configuration XML schema version

Enforce a certain version of the jOOQ configuration XML schema by declaring what version of the jOOQ code generation tool to
make available to the jOOQ plugin at configuration time, i.e. in the DSL of the jOOQ plugin.

### Gradle Groovy DSL

```groovy
buildscript {
    configurations['classpath'].resolutionStrategy.eachDependency {
        if (requested.group == 'org.jooq' && requested.name == 'jooq-codegen') {
            useVersion '3.12.4'
        }
    }
}
```

### Gradle Kotlin DSL

```kotlin
buildscript {
    configurations["classpath"].resolutionStrategy.eachDependency {
        if (requested.group == "org.jooq") {
            useVersion("3.12.4")
        }
    }
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

By default, the code generation tasks are automatically configured as dependencies of the corresponding source compilation tasks
provided by the `JavaBasePlugin` plugin. Hence, running a build that eventually needs to compile sources will first
trigger the required jOOQ code generation tasks. This auto-triggering of the code generation when compiling the
containing source set can be turned off by setting `generateSchemaSourceOnCompilation` to `false`.

You can delete all files in the folder that you configure as the output directory of the code generation task by running the
Gradle task rule `cleanGenerate[ConfigurationName]JooqSchemaSource`. Note that this task rule will delete _all_ files in the
configured output folder, regardless of whether the files were generated by the jOOQ plugin or not.

```console
gradle cleanGenerateSampleJooqSchemaSource
```

To see the log output of the jOOQ code generation tool, run the Gradle build with log level `info`:

```console
gradle build -i
```

# Configuration

The example below shows a jOOQ configuration that creates the jOOQ Java sources from a PostgreSQL database schema and
includes them in the `main` source set.

By default, the generated sources are written to `build/generated-src/jooq/<configurationName>`. The
output directory can be configured by explicitly setting the `directory` attribute of the `target` configuration.

See the [jOOQ XSD](https://www.jooq.org/xsd/jooq-codegen-3.13.0.xsd) for the full set of configuration options.

```groovy
jooq {
  version = '3.13.4'
  edition = 'OSS'
  generateSchemaSourceOnCompilation = true
  sample(sourceSets.main) {
    jdbc {
      driver = 'org.postgresql.Driver'
      url = 'jdbc:postgresql://localhost:5432/sample'
      user = 'some_user'
      password = 'secret'
      properties {
        property {
          key = 'ssl'
          value = 'true'
        }
      }
    }
    generator {
      name = 'org.jooq.codegen.DefaultGenerator'
      strategy {
        name = 'org.jooq.codegen.DefaultGeneratorStrategy'
        // ...
      }
      database {
        name = 'org.jooq.meta.postgres.PostgresDatabase'
        inputSchema = 'public'
        forcedTypes {
          forcedType {
            name = 'varchar'
            expression = '.*'
            types = 'JSONB?'
          }
          forcedType {
            name = 'varchar'
            expression = '.*'
            types = 'INET'
          }
        }
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

## Configuration pitfalls

### Configuring a sequence of elements

Resemblance of the jOOQ configuration DSL with the Groovy language is coincidental. Complex types that include
sequences like [ForcedTypes](https://www.jooq.org/xsd/jooq-codegen-3.13.0.xsd) must be defined in the DSL's nesting style:

```groovy
forcedTypes {
  forcedType {
    name = 'varchar'
    expression = '.*'
    types = 'JSONB?'
  }
  forcedType {
    name = 'varchar'
    expression = '.*'
    types = 'INET'
  }
}
```

The Groovy list style is **not** supported:

```groovy
forcedTypes = [
  {
    name = 'varchar'
    expression = '.*'
    types = 'JSONB?'
  },
  {
    name = 'varchar'
    expression = '.*'
    types = 'INET'
  }
]
```

### Defining the jOOQ version when the Spring boot plugin is applied

When applying the [spring-boot-gradle-plugin](https://github.com/spring-projects/spring-boot/tree/master/spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin),
it is not sufficient to declare the jOOQ version that you want to pull in via `jooq.version = '3.13.4'` since the dependency management rules of the spring-boot-gradle-plugin
take precedence. You also have to set `ext['jooq.version'] = '3.13.4'` to pull in your requested version of jOOQ.

### Generating sources into shared folders, e.g. src/main/java

My recommendation is to generate the jOOQ sources into a distinct folder, e.g. _src/generated/jooq_ or _build/generated-src/jooq_ (default). This avoids overlapping
outputs, and it also keeps the door open to let Gradle cache the generated sources which can be a significant build performance gain. The rationale is explained very
well in the [Build Cache User Guide](https://guides.gradle.org/using-build-cache/#concepts_overlapping_outputs).

# Invocation

## Invoke Jooq task

You can generate the Java jOOQ sources for a given jOOQ configuration by invoking the command `generate<configName>Jooq`, e.g. `generateTestJooq`. The only exception
being _main_ that is abbreviated to `generateJooq`, similarly to how it is done for the `JavaCompile` tasks contributed by the `java` plugin.

# Examples

+ Configuring the jOOQ code generation via Gradle Groovy DSL: [here](example/use_groovy_dsl).
+ Configuring the jOOQ code generation via Gradle Kotlin DSL: [here](example/use_kotlin_dsl).
+ Passing JVM args to the jOOQ code generation process: [here](example/configure_jvm_args).
+ Using a custom generator strategy defined in the same Gradle multi-module project: [here](example/configure_custom_generator_strategy).
+ Suppressing the task dependency between the compile task and the jOOQ source generation task: [here](example/configure_task_dependencies).
+ Specifying applied jOOQ config XML schema version: [here](example/specify_jooq_config_xml_schema_version).

# Changelog

+ 5.0.1 - Support dependency substitution to use different versions of the jOOQ dependencies than those pulled in by the jOOQ plugin.
+ 5.0 - Change DSL. Support Gradle Kotlin DSL. Add normalization hook. Make Gradle 6.1 the minimum compatible version. Upgrade to jOOQ 3.13.4.
+ 4.2 - Add new jOOQ editions for Java 8 and Java 6. Upgrade to jOOQ 3.13.1.
+ 4.1 - Global flag to turn off auto-generation of jOOQ schema source when compiling the containing source set
+ 4.0 - Make Gradle 5.0 the minimum compatible version. Upgrade to jOOQ 3.12.3.
+ 3.0.3 - Explicitly add JAXB dependencies to run on JDK 9 and higher out-of-the-box. Upgrade to jOOQ 3.11.9.
+ 3.0.2 - Bug fix when running on JDK 9+
+ 3.0.1 - Improve Gradle build cache effectiveness of the jOOQ task
+ 3.0.0 - Upgrade to jOOQ 3.11.2 (jOOQ 3.11.x breaks compatibility with jOOQ 3.10.x)
+ 2.0.11 - Upgrade to jOOQ 3.10.4
+ 2.0.10 - Removal of wiring between clean task and deleting generated jOOQ sources
+ 2.0.9 - Make jOOQ 3.10.1 the default applied version
+ 2.0.8 - Upgrade to jOOQ 3.10.1
+ 2.0.7 - Upgrade to jOOQ 3.9.5
+ 2.0.6 - Upgrade to jOOQ 3.9.3
+ 2.0.5 - Make the jOOQ task parallelizable
+ 2.0.4 - Upgrade to jOOQ 3.9.1 and better configuration error messages
+ 2.0.3 - Upgrade to jOOQ 3.9.0
+ 2.0.2 - Configuration of call-backs for code generation java execution process
+ 2.0.1 - Bug fixes
+ 2.0.0 - Make jOOQ version used for code generation independent from jOOQ version used by gradle-jooq plugin
+ 1.0.6 - Upgrade to jOOQ 3.6.2

# Feedback and Contributions

Both feedback and contributions are very welcome.

# Acknowledgements

+ [wolfs](https://github.com/wolfs) (design)
+ [Sineaggi](https://github.com/Sineaggi) (pr)
+ [martintreurnicht](https://github.com/martintreurnicht) (pr)
+ [anuraaga](https://github.com/anuraaga) (pr)
+ [ldaley](https://github.com/ldaley) (pr)
+ [masc3d](https://github.com/masc3d) (pr)
+ [mark-vieira](https://github.com/mark-vieira) (pr)
+ [felipefzdz](https://github.com/felipefzdz) (commits)
+ [oehme](https://github.com/oehme) (pr)
+ [jamespedwards42](https://github.com/jamespedwards42) (idea)
+ [dubacher](https://github.com/dubacher) (patch)
+ [lukaseder](https://github.com/lukaseder) (patch)

# License

This plugin is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

(c) by Etienne Studer
