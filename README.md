<p align="left">
  <a href="https://github.com/etiennestuder/gradle-jooq-plugin/actions?query=workflow%3A%22Build+Gradle+project%22"><img src="https://github.com/etiennestuder/gradle-jooq-plugin/workflows/Build%20Gradle%20project/badge.svg"></a>
</p>

gradle-jooq-plugin
==================

> The work on this software project is in no way associated with my employer nor with the role I'm having at my employer. Any requests for changes will be decided upon exclusively by myself based on my personal preferences. I maintain this project as much or as little as my spare time permits.

# Overview
[Gradle](http://www.gradle.org) plugin that integrates the jOOQ code generation tool.

For each named jOOQ configuration declared in the build, the plugin adds a task to generate the jOOQ sources from the specified database schema and includes the
generated Java sources in the matching source set, if existing. The code generation tasks participate
in [task configuration avoidance](https://docs.gradle.org/current/userguide/task_configuration_avoidance.html),
in [build configuration caching](https://docs.gradle.org/nightly/userguide/configuration_cache.html),
in [incremental builds](https://docs.gradle.org/current/userguide/more_about_tasks.html#sec:up_to_date_checks),
in [task output caching](https://docs.gradle.org/current/userguide/build_cache.html),
and in [toolchains](https://docs.gradle.org/current/userguide/toolchains.html). The plugin can be applied on both Java projects and Android projects.

You can find more details about the actual jOOQ source code generation in the [jOOQ documentation](http://www.jooq.org/doc/latest/manual/code-generation).

The jOOQ plugin is hosted at the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/nu.studer.jooq).

## Build scan

Recent build scan: https://gradle.com/s/zcltzipq7zzha

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
 * `JooqGenerate` task instances participate in incremental builds (if the task gets explicitly marked as all inputs being declared)
 * `JooqGenerate` task instances participate in task output caching (if the task gets explicitly marked as all inputs being declared)
 * `JooqGenerate` task instances participate in toolchains (if the task or project is configured with a toolchain)

# Compatibility

| Plugin version | Compatible Gradle versions | Support for Gradle Kotlin DSL |Support for Gradle Configuration Cache| Minimum JDK | Minimum jOOQ |
|----------------|----------------------------|-------------------------------|--------------------------------------|-------------|--------------|
| 8.0+           | 7.0+                       | Yes                           | Yes                                  | 17          | 3.16+        |
| 7.0+           | 6.1+, 7.0+                 | Yes                           | Yes                                  | 11          | 3.16+        |
| 6.0+           | 6.1+, 7.0+                 | Yes                           | Yes                                  | 11          | <= 3.15      |
| 5.0+           | 6.1+, 7.0+                 | Yes                           | Yes                                  | 8           | <= 3.15      |
| 4.0            | 5.0+, 6.0+, 7.0+           | No                            | No                                   | 8           | <= 3.15      |

See the [Migration](#migration) section on how to migrate your build from older to newer jOOQ plugin versions.

# Configuration

## Applying the plugin

Apply the `nu.studer.jooq` plugin to your Gradle project.

### Gradle Groovy DSL

```groovy
plugins {
    id 'nu.studer.jooq' version '8.0'
}
```

### Gradle Kotlin DSL

```kotlin
plugins {
    id("nu.studer.jooq") version "8.0"
}
```

## Adding the database driver

Add the database driver of the database that the jOOQ code generation tool will introspect to the `jooqGenerator` configuration. This ensures that the database driver
is on the classpath when the jOOQ code generation tool is executed. Optionally, you can add additional dependencies that are required to run the jOOQ code generation tool.

### Gradle Groovy DSL

```groovy
dependencies {
    jooqGenerator 'org.postgresql:postgresql:42.5.0'
}
```

### Gradle Kotlin DSL

```kotlin
dependencies {
    jooqGenerator("org.postgresql:postgresql:42.5.0")
}
```

## Specifying the jOOQ version and edition

Specify the version and [edition](https://github.com/etiennestuder/gradle-jooq-plugin/blob/master/src/main/groovy/nu/studer/gradle/jooq/JooqEdition.java) that should be applied to all jOOQ dependencies that are declared in your project either explicitly or pulled in transitively.

Note that the `org.jooq:jooq` dependency of the specified version and edition is automatically added to the `implementation` configuration of the source set that matches the name of the declared jOOQ configuration.

### Gradle Groovy DSL

```groovy
jooq {
  version = '3.17.5'  // the default (can be omitted)
  edition = nu.studer.gradle.jooq.JooqEdition.OSS  // the default (can be omitted)
}
```

### Gradle Kotlin DSL

```kotlin
jooq {
  version.set("3.17.5")  // the default (can be omitted)
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
        if (requested.group == 'org.jooq') {
            useVersion '3.16.1'
        }
    }
}
```

### Gradle Kotlin DSL

```kotlin
buildscript {
    configurations["classpath"].resolutionStrategy.eachDependency {
        if (requested.group == "org.jooq") {
            useVersion("3.16.1")
        }
    }
}
```

## Configuring the jOOQ generation tool

Configure the jOOQ generation tool via `jooq` extension, made available by the jOOQ plugin. The full set of configuration options when using jOOQ 3.17.x can
be seen on the jOOQ generation tool's [Configuration](https://github.com/jOOQ/jOOQ/tree/version-3.17.5/jOOQ-meta/src/main/java/org/jooq/meta/jaxb) class, or
on the [jOOQ XSD](https://www.jooq.org/xsd/jooq-codegen-3.17.0.xsd).

By default, the generated sources are written to `<projectDir>/build/generated-src/jooq/<configurationName>`. The target directory can be changed by
explicitly setting the `directory` attribute of the `target` configuration of the `generator` configuration.

### Gradle Groovy DSL

```groovy
import org.jooq.meta.jaxb.Logging

jooq {
    version = '3.17.5'  // default (can be omitted)
    edition = nu.studer.gradle.jooq.JooqEdition.OSS  // default (can be omitted)

    configurations {
        main {  // name of the jOOQ configuration
            generateSchemaSourceOnCompilation = true  // default (can be omitted)

            generationTool {
                logging = Logging.WARN
                jdbc {
                    driver = 'org.postgresql.Driver'
                    url = 'jdbc:postgresql://localhost:5432/sample'
                    user = 'some_user'
                    password = 'some_secret'
                    properties {
                        property {
                            key = 'ssl'
                            value = 'true'
                        }
                    }
                }
                generator {
                    name = 'org.jooq.codegen.DefaultGenerator'
                    database {
                        name = 'org.jooq.meta.postgres.PostgresDatabase'
                        inputSchema = 'public'
                        forcedTypes {
                            forcedType {
                                name = 'varchar'
                                includeExpression = '.*'
                                includeTypes = 'JSONB?'
                            }
                            forcedType {
                                name = 'varchar'
                                includeExpression = '.*'
                                includeTypes = 'INET'
                            }
                        }
                    }
                    generate {
                        deprecated = false
                        records = true
                        immutablePojos = true
                        fluentSetters = true
                    }
                    target {
                        packageName = 'nu.studer.sample'
                        directory = 'build/generated-src/jooq/main'  // default (can be omitted)
                    }
                    strategy.name = 'org.jooq.codegen.DefaultGeneratorStrategy'
                }
            }
        }
    }
}
```

See the [Examples](#examples) section for complete, exemplary build scripts that apply the jOOQ plugin.

### Gradle Kotlin DSL

```kotlin
import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.Property

jooq {
    version.set("3.17.5")  // default (can be omitted)
    edition.set(nu.studer.gradle.jooq.JooqEdition.OSS)  // default (can be omitted)

    configurations {
        create("main") {  // name of the jOOQ configuration
            generateSchemaSourceOnCompilation.set(true)  // default (can be omitted)

            jooqConfiguration.apply {
                logging = Logging.WARN
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:5432/sample"
                    user = "some_user"
                    password = "some_secret"
                    properties.add(Property().apply {
                        key = "ssl"
                        value = "true"
                    })
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        forcedTypes.addAll(listOf(
                            ForcedType().apply {
                                name = "varchar"
                                includeExpression = ".*"
                                includeTypes = "JSONB?"
                            },
                            ForcedType().apply {
                                name = "varchar"
                                includeExpression = ".*"
                                includeTypes = "INET"
                            }
                        ))
                    }
                    generate.apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = true
                        isFluentSetters = true
                    }
                    target.apply {
                        packageName = "nu.studer.sample"
                        directory = "build/generated-src/jooq/main"  // default (can be omitted)
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}
```

See the [Examples](#examples) section for complete, exemplary build scripts that apply the jOOQ plugin.

## Configuring the jOOQ generation task to participate in incremental builds and build caching

If you configure the state of the database schema from which to derive the jOOQ sources as an input to the jOOQ task, you can mark the
jOOQ task as having all its inputs declared by setting the `allInputsDeclared` task property to `true`. The jOOQ task will then participate
in Gradle's incremental build and build caching features. The `allInputsDeclared` task property is `false` by default.

See [here](example/configure_incremental_build_and_build_caching_participation) for a complete example on how to enable participation in
incremental build and build caching.

### Gradle Groovy DSL

```groovy
    tasks.named('generateJooq').configure { allInputsDeclared = true }
```

### Gradle Kotlin DSL

```kotlin
    tasks.named<nu.studer.gradle.jooq.JooqGenerate>("generateJooq") { allInputsDeclared.set(true) }
```

## Configuring the jOOQ generation task with a toolchain

If you configure a toolchain on the project to which the jOOQ task belongs, it is automatically used by the jOOQ task. You
can also configure / override the toolchain on the jOOQ task itself.

### Gradle Groovy DSL

```groovy
    tasks.named('generateJooq').configure {
        launcher = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(18)
        }
    }
```

See [here](example/configure_toolchain_gradle_dsl) for a complete example on how to configure the toolchain to be used by the jOOQ task, using the Gradle DSL.

### Gradle Kotlin DSL

```kotlin
    tasks.named<nu.studer.gradle.jooq.JooqGenerate>("generateJooq") {
        (launcher::set)(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(18))
        })
    }
```
Note: `(launcher::set)(...)` is a necessary workaround to deal with an ambiguous overloading issue in the Kotlin compiler.

See [here](example/configure_toolchain_kotlin_dsl) for a complete example on how to configure the toolchain to be used by the jOOQ task, using the Kotlin DSL.

## Avoiding configuration pitfalls

### Synchronizing the jOOQ version between the Spring Boot Gradle plugin and the jOOQ Gradle plugin

It is advisable that the jOOQ Gradle plugin and the [Spring Boot Gradle plugin](https://github.com/spring-projects/spring-boot/tree/master/spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin) are
configured to use the same version of jOOQ.

If you want the Spring Boot plugin to pull in the same version of jOOQ as defined by the jOOQ plugin, you have to explicitly set `ext['jooq.version'] = jooq.version.get()`.

The other way around, if you want the jOOQ plugin to pull in the same version of jOOQ as defined by the Spring Boot plugin, you have to explicitly
set `jooq.version = dependencyManagement.importedProperties['jooq.version']`.

### Enforcing dependency versions via dependency rules from third-party plugins or from the build itself

If the code generation fails with exceptions about not finding certain JAXB classes, it is likely due to a 3rd-party plugin or your own build
adding some dependency rules that enforce certain dependency versions that are not matching what is needed by the jOOQ code generation tool. For
example, the Spring Dependency Management Gradle plugin will downgrade the `jakarta.xml.bind:jakarta.xml.bind-api` dependency to a version not
compatible with the jOOQ code generation tool and leads to the error below. This [issue](https://github.com/etiennestuder/gradle-jooq-plugin/issues/209)
provides some insights on how to debug such cases.

`Exception in thread "main" java.lang.NoClassDefFoundError: jakarta/xml/bind/annotation/XmlSchema`

### Generating sources into shared folders, e.g. src/main/java

My recommendation is to generate the jOOQ sources into a distinct folder, e.g. _src/generated/jooq_ or _build/generated-src/jooq_ (default). This avoids overlapping
outputs, and it also keeps the door open to let Gradle cache the generated sources which can be a significant build performance gain. The rationale is explained very
well in the [Build Cache User Guide](https://guides.gradle.org/using-build-cache/#concepts_overlapping_outputs).

### Configuring a sequence of elements using the Gradle Groovy DSL

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

### Working with Configurations in the Kotlin DSL

See [here](KotlinDSL.md) for additional insights on configuring the jOOQ code generation tool using the Gradle Kotlin DSL.

# Execution

## Generating the jOOQ sources

You can generate the jOOQ sources for a given jOOQ configuration by invoking the task `generate<configName>Jooq`, e.g. `generateTestJooq`. The only exception
being _main_ that is abbreviated to `generateJooq`, similarly to how it is done for the `JavaCompile` tasks contributed by the `java` plugin. The generated jOOQ
sources are automatically added to the source set with the name that matches the name of the given jOOQ configuration.

```console
./gradlew generateJooq
```

By default, the code generation tasks are automatically configured as dependencies of the corresponding source compilation tasks provided by the `JavaBasePlugin` plugin. Hence,
running a build that eventually needs to compile sources will first trigger the required jOOQ code generation tasks. This auto-triggering of the code generation when compiling
the containing source set can be turned off by setting `generateSchemaSourceOnCompilation` to `false` on the jOOQ configuration.

## Deleting the generated jOOQ sources

You can delete the generated jOOQ sources by invoking the task rule `cleanGenerate<configName>Jooq`, e.g. `cleanGenerateTestJooq`. The only exception
being _main_ that is abbreviated to `cleanGenerateJooq`, similarly to how it is done for the `JavaCompile` tasks contributed by the `java` plugin. The
task rule will delete all files in the folder that is configured as the destination directory, regardless of whether the files were generated by the
jOOQ plugin or not.

```console
./gradlew cleanGenerateJooq
```

# Migration

## Migrating from jOOQ plugin 7.x to 8.x

When migrating your build from jOOQ plugin 7.x to 8.x, follow these steps:

- Ensure you run the Gradle build with at least JDK 17

## Migrating from jOOQ plugin 6.x to 7.x

When migrating your build from jOOQ plugin 6.x to 7.x, follow these steps:

- Ensure you configure the gradle-jooq-plugin with at least jOOQ version 3.16.0 (or just use the implicit default)

## Migrating from jOOQ plugin 5.x to 6.x

When migrating your build from jOOQ plugin 5.x to 6.x, follow these steps:

- Ensure you run the Gradle build with at least JDK 11

## Migrating from jOOQ plugin 4.x to 5.x

When migrating your build from jOOQ plugin 4.x to 5.x, follow these steps:

- Rename the configuration provided the jOOQ plugin from `jooqRuntime` to `jooqGenerator`
- Set the `edition` property as a [JooqEdition](https://github.com/etiennestuder/gradle-jooq-plugin/blob/master/src/main/groovy/nu/studer/gradle/jooq/JooqEdition.java) enum
 value instead of a String value
- Wrap the entirety of your jOOQ configurations with a `configurations` block
- Rename the jOOQ configuration to the name of the previously passed source set
- Move the `generateSchemaSourceOnCompilation` property assignment from the `jooq` block to the desired jOOQ configuration
- Wrap the configuration of the generation tool with a `generationTool` block
- Rename any references to the jOOQ task type from `JooqTask` to `JooqGenerate`
- Rename any references to the jOOQ tasks from `generate<configName>JooqSchemaSource` to `generate<configName>Jooq`

# Examples

+ Configuring the jOOQ code generation via Gradle Groovy DSL: [here](example/use_groovy_dsl).
+ Configuring the jOOQ code generation via Gradle Kotlin DSL: [here](example/use_kotlin_dsl).
+ Extracting the jOOQ configuration into a script file: [here](example/extract_script_file).
+ Extracting the jOOQ configuration into a precompiled script plugin: [here](example/extract_precompiled_script_plugin).
+ Passing JVM args to the jOOQ code generation process: [here](example/configure_jvm_args).
+ Configuring a JVM toolchain to run the jOOQ code generation process via Gradle DSL: [here](example/configure_toolchain_gradle_dsl).
+ Configuring a JVM toolchain to run the jOOQ code generation process via Kotlin DSL: [here](example/configure_toolchain_kotlin_dsl).
+ Using a custom generator strategy defined in the same Gradle multi-module project: [here](example/configure_custom_generator_strategy).
+ Suppressing the task dependency between the compile task and the jOOQ source generation task: [here](example/configure_task_dependencies).
+ Declaring multiple configurations whose outputs are all added to the same source set: [here](example/configure_different_jooq_sources_for_same_target_source_set).
+ Participating in incremental build and build caching: [here](example/configure_incremental_build_and_build_caching_participation).
+ Customizing the execution of the code generation tool: [here](example/configure_generation_tool_execution).
+ Normalizing the jOOQ config to ensure relocatability: [here](example/configure_jooq_config_normalization).
+ Specifying applied jOOQ config XML schema version: [here](example/specify_jooq_config_xml_schema_version).
+ Using Spring Boot's jOOQ version in the jOOQ plugin: [here](example/configure_jooq_version_from_spring_boot).
+ Using Flyway in combination with jOOQ to generate the schema and jOOQ sources: [here](example/configure_jooq_with_flyway).

# Changelog

+ Next - Upgrade to jOOQ 3.17.5.
+ 8.0 - Make Gradle 7.0 the minimum compatible version. Make Java 17 the minimum version. Upgrade to jOOQ 3.17.4.
+ 7.1.1 - Upgrade to jOOQ 3.16.4
+ 7.1 - Add support for Gradle Toolchains.
+ 7.0 - Upgrade to jOOQ 3.16.3 and make jOOQ 3.16.x the minimum version. Update used 3rd-party dependencies.
+ 6.0.1 - Avoid deprecation warnings at Gradle runtime. Upgrade to jOOQ 3.15.1.
+ 6.0 - Make Java 11 the minimum version. Upgrade to jOOQ 3.15.0.
+ 5.2.2 - Upgrade to jOOQ 3.14.11
+ 5.2.1 - Upgrade to jOOQ 3.14.7
+ 5.2 - Fail build if cleaning of output directory is set to false in the jOOQ configuration. Upgrade to jOOQ 3.13.5.
+ 5.1.1 - Expose org.jooq:jooq-codegen library as `compile` dependency instead of `runtime` dependency
+ 5.1.0 - Require explicit opt-in to participate in incremental builds
+ 5.0.3 - Clean output directory before generating jOOQ sources
+ 5.0.2 - Do not write out JDBC configuration when empty
+ 5.0.1 - Support dependency substitution to use different versions of jOOQ dependencies than those pulled in by the jOOQ plugin
+ 5.0 - Change DSL. Support Gradle Kotlin DSL. Add normalization hook. Make Gradle 6.1 the minimum version. Upgrade to jOOQ 3.13.4.
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

+ [alextu](https://github.com/alextu) (pr)
+ [rpalcolea ](https://github.com/rpalcolea) (pr)
+ [mrozanc](https://github.com/mrozanc) (pr)
+ [perlun](https://github.com/perlun) (pr)
+ [Double-O-Seven](https://github.com/Double-O-Seven) (issue analysis)
+ [wolfs](https://github.com/wolfs) (design)
+ [jonnybbb](https://github.com/jonnybbb) (pr)
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
