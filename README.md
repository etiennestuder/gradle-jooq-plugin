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

Groovy DSL
```groovy
plugins {
    id 'nu.studer.jooq' version '3.0.3'
}
```
Kotlin DSL
```kotlin
plugins {
    id("nu.studer.jooq") version "3.0.2"
}
```

Or using the `buildscript` block

Groovy DSL
```groovy
buildscript {
  repositories {
    maven {
      url 'https://plugins.gradle.org/m2/'
    }
  }
  dependencies {
    classpath 'nu.studer:gradle-jooq-plugin:3.0.3'
  }
}

apply plugin: 'nu.studer.jooq'
```

Kotlin DSL
```kotlin
buildscript {
  repositories {
    maven(url = "https://plugins.gradle.org/m2/")
  }
  dependencies {
    classpath("nu.studer:gradle-jooq-plugin:3.0.2")
  }
}

apply(plugin = "nu.studer.jooq")
```


**Please note that due to non-backward compatible API changes in jOOQ between 3.10.x and 3.11.x, you must apply the following plugin version in your Gradle build:**
* **jOOQ library <= 3.10.x: gradle-jooq plugin 2.0.11** 
* **jOOQ library >= 3.11.x: gradle-jooq plugin 3.0.0 or higher**
  
# Defining your database drivers

Depending on which database you are connecting to, you need to put the corresponding driver on the generator's classpath.

Groovy DSL
```groovy
dependencies {
    jooqRuntime 'postgresql:postgresql:9.1-901.jdbc4'
}
```

Kotlin DSL
```kotlin
dependencies {
    jooqRuntime("postgresql:postgresql:9.1-901.jdbc")
}
```

# Specifying the jOOQ version and edition

This plugin supports existing and future jOOQ versions. It also supports the different editions like open source, pro, and trial.

Groovy DSL
```groovy
jooq {
  version = '3.11.9' // the default (can be omitted)
  edition = 'OSS'    // the default (can be omitted), other allowed values are PRO, PRO_JAVA_6, and TRIAL
}
```

Kotlin DSL
```kotlin
import nu.studer.gradle.jooq.*
import nu.studer.gradle.jooq.JooqEdition

jooq {
  version = "3.11.2" // the default (can be omitted)
  edition = JooqEdition.OSS    // the default (can be omitted), other allowed values are PRO, PRO_JAVA_6, and TRIAL
}
```

The plugin ensures that all your dependencies use the version and edition
specified in the `jooq` configuration. So when you declare a compile dependency
on jOOQ, you can omit the version:

Groovy DSL
```groovy
dependencies {
  compile 'org.jooq:jooq'
}
```

Kotlin DSL
```kotlin
dependencies {
  compile("org.jooq:jooq")
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

See the [jOOQ XSD](https://www.jooq.org/xsd/jooq-codegen-3.11.0.xsd) for the full set of configuration options.

Groovy DSL
```groovy
jooq {
  version = '3.11.9'
  edition = 'OSS'
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

Kotlin DSL
```kotlin
import nu.studer.gradle.jooq.*
import nu.studer.gradle.jooq.JooqEdition

jooq {
  version = "3.11.2"
  edition = JooqEdition.OSS
  "sample"(sourceSet["main"]) {
    jdbc {
      driver = "org.postgresql.Driver"
      url = "jdbc:postgresql://localhost:5432/sample"
      user = "some_user"
      password = "secret"
      properties {
        property {
          key = "ssl"
          value = "true"
        }
      }
    }
    generator {
      name = "org.jooq.codegen.DefaultGenerator"
      strategy {
        name = "org.jooq.codegen.DefaultGeneratorStrategy"
        // ...
      }
      database {
        name = "org.jooq.meta.postgres.PostgresDatabase"
        inputSchema = "public"
        forcedTypes {
          forcedType {
            name = "varchar"
            expression = ".*"
            types = "JSONB?"
          }
          forcedType {
            name = "varchar"
            expression = ".*"
            types = "INET"
          }
        }
        // ...
      }
      generate {
        isRelations = true
        isDeprecated = false
        isRecords = true
        isImmutablePojos = true
        isFluentSetters = true
        // ...
      }
      target {
        packageName = "nu.studer.sample"
        // directory = ...
      }
    }
  }
}
```

## Configuration pitfalls

### Configuring a sequence of elements

Resemblance of the jOOQ configuration DSL with the Groovy language is coincidental. Complex types that include 
sequences like [ForcedTypes](https://www.jooq.org/xsd/jooq-codegen-3.11.0.xsd) must be defined in the DSL's nesting style:

Groovy DSL
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

Kotlin DSL
```kotlin
import nu.studer.gradle.jooq.*

forcedTypes {
  forcedType {
    name = "varchar"
    expression = ".*"
    types = "JSONB?"
  }
  forcedType {
    name = "varchar"
    expression = ".*"
    types = "INET"
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

### Defining matchers

When using `matchers`, the `name` element must be set to `null` explicitly:

Groovy DSL
```groovy
strategy {
  name = null
  matchers {
    tables {
      table {
        pojoClass {
          transform = 'PASCAL'
          expression = '\$0_POJO' 
        }
      }
    }
  }
}
```

Kotlin DSL
```kotlin
import org.jooq.meta.jaxb.MatcherTransformType
import nu.studer.gradle.jooq.*

strategy {
  name = null
  matchers {
    tables {
      table {
        pojoClass {
          transform = MatcherTransformType.PASCAL
          expression = '\$0_POJO' 
        }
      }
    }
  }
}
```

Background: the plugin consumes JAXB classes generated from the [jOOQ XSD](https://www.jooq.org/xsd/jooq-codegen-3.11.0.xsd). The `name` on the `Strategy` element 
has a default value and that's an issue since is part of an XSD `choice` element, i.e. only one element can be present. This is the only `choice` element 
in the whole XSD, so this workaround only needs to be applied here.

### Defining the jOOQ version when the Spring boot plugin is applied

When applying the [spring-boot-gradle-plugin](https://github.com/spring-projects/spring-boot/tree/master/spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin), 
it is not sufficient to declared the jOOQ version that you want to pull in via `jooq.version = '3.11.9'` since the dependency management rules of the spring-boot-gradle-plugin 
take precedence. You also have to set `ext['jooq.version'] = '3.11.9'` to pull in your requested version of jOOQ.

### Generating sources into shared folders, e.g. src/main/java 

My recommendation is to generate the jOOQ sources into a distinct folder, e.g. _src/generated/jooq_ or _build/generated-src/jooq_ (default). This avoids overlapping 
outputs, and it also keeps the door open to let Gradle cache the generated sources which can be a significant build performance gain. The rationale is explained very 
well in the [Build Cache User Guide](https://guides.gradle.org/using-build-cache/#concepts_overlapping_outputs).
 
# Samples

+ Passing JVM args to the jOOQ code generation process: [here](example/add_jvm_args).  
+ Removing the implicit task dependency between the compile task and the jOOQ source generation task: [here](example/remove_task_dependency).  
+ Using a custom generator strategy defined in the same Gradle project: [here](example/use_custom_generator).    
+ Running on JDK 9 and higher with all JAXB dependencies already added by the plugin: [here](example/run_jdk9).    

# Changelog
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
+ 2.0.0 - jOOQ version used for the code generation is independent from the jOOQ version used by the gradle-jooq plugin
+ 1.0.6 - Upgrade to jOOQ 3.6.2

# Acknowledgements

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
