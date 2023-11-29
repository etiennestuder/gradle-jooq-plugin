## Working with Configurations using the Gradle Kotlin DSL

Each configuration `create()`ed under the `jooq { configurations { } }` block presents the script author with an opportunity to customize a tree of JAXB elements drawn
from `jooq-meta` which encodes the [JOOQ Codegen XML Schema](https://www.jooq.org/xsd/jooq-codegen-3.18.0.xsd).

For each configuration so configured, this tree is serialized to a temporary XML file and passed as a command-line argument to `org.jooq.codegen.GenerationTool` for code
generation.

Before your Gradle Kotlin DSL script configures it, each configuration's JAXB tree is pre-populated as follows:

```groovy
        // all types appearing below are from package org.jooq.meta.jaxb
        return new Configuration()
            .withJdbc(new Jdbc())
            .withGenerator(new Generator()
                .withStrategy(new Strategy())
                .withDatabase(new Database())
                .withGenerate(new Generate())
                .withTarget(new Target()
                    .withDirectory(null)
                )
            )
```

The act of customizing this tree can be made DSL-like using the `apply()` extension-method from the Kotlin standard library:

```kotlin
import org.jooq.meta.jaxb.Logging
jooqConfiguration.apply {
    // this: org.jooq.meta.jaxb.Configuration
    logging = Logging.WARN  // simple assignment of single-valued property
    jdbc.apply {  // 'jdbc' property that was previously instantiated for you
        // this: org.jooq.meta.jaxb.Jdbc
        driver = "org.h2.Driver"
        url = "jdbc:h2:~/test;AUTO_SERVER=TRUE"
    }
}
```

However, for collection-valued properties, they are initially-empty, and you must:

1. Instantiate a set of JAXB elements of the correct type
2. Set (or add) them as a collection

In the example below, `forcedTypes` is an initially-empty `List<org.jooq.meta.jaxb.ForcedType>` member of `org.jooq.meta.jaxb.Database` that
is replaced with a collection of two elements:

```kotlin
import org.jooq.meta.jaxb.ForcedType
jooqConfiguration.apply {
    generator.apply {
        database.apply {
            forcedTypes = listOf(
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
            )
        }
    }
}
```

Familiarity with the [JOOQ Codegen XML Schema](https://www.jooq.org/xsd/jooq-codegen-3.18.0.xsd) and
the [`org.jooq.meta.jaxb` package](https://github.com/jOOQ/jOOQ/tree/version-3.18.7/jOOQ-meta/src/main/java/org/jooq/meta/jaxb)
will inform your configuration efforts.
