## Working with Configurations using the Gradle Kotlin DSL

Each configuration `create()`ed under the `jooq { configurations { } }` block presents the script author with an opportunity to customize a tree of JAXB elements drawn
from `jooq-meta` which encodes the [JOOQ Codegen XML Schema](https://www.jooq.org/xsd/jooq-codegen-3.16.0.xsd).

For each configuration so configured, this tree is serialized to a temporary XML file and passed as a command-line argument to `org.jooq.codegen.GenerationTool` for code
generation.

Before your Gradle Kotlin DSL script configures it, each configuration's JAXB tree is pre-populated as follows:

```groovy
        // all types appearing below are from org.jooq.meta.jaxb
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
jooqConfiguration.apply {
    // this: org.jooq.meta.jaxb.Configuration
    logging = Logging.WARN  // simple assignment of single-valued property of o.j.m.j.Configuration
    jdbc.apply {  // 'jdbc' property that was previously instantiated for you
        // this: org.jooq.meta.jaxb.Jdbc
        driver = "org.postgresql.Driver"
    }
}
```

However, for collection-valued properties, they are initially-empty, and you must:

* instantiate a JAXB element of the correct type
* add it to the collection

In the example below, `forcedTypes` is an initially-empty `List<org.jooq.meta.jaxb.ForcedType>` member of `org.jooq.meta.jaxb.Database`:

```kotlin
forcedTypes.addAll(listOf(
 ForcedType()
  .withName("varchar")
  .withIncludeExpression(".*")
  .withIncludeTypes("JSONB?"),
 ForcedType()
  .withName("varchar")
  .withIncludeExpression(".*")
  .withIncludeTypes("INET")
).listOf())
```

In the next example, `schemata` is an initially-empty `List<org.jooq.meta.jaxb.SchemaMappingType>` member of `org.jooq.meta.jaxb.Database`:

```kotlin
   schemata.addAll(listOf(
       SchemaMappingType().apply {  // using 'apply' for consistency
           inputSchema = "foo"
       },
       SchemaMappingType().withInputSchema("bar")  // using withXXX() for brevity
                                                   // possible only because o.j.m.j.SchemaMappingType happens to be fluent
                                                   // and not because of any Kotlin extension-method offered by this plugin
   ))
```

Familiarity with the [JOOQ Codegen XML Schema](https://www.jooq.org/xsd/jooq-codegen-3.16.0.xsd) and
the [`org.jooq.meta.jaxb` package](https://github.com/jOOQ/jOOQ/tree/main/jOOQ-meta/src/main/java/org/jooq/meta/jaxb)
will inform your configuration efforts.
