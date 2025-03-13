import nu.studer.gradle.jooq.JooqEdition
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.Property
import org.jooq.meta.jaxb.ForcedType
import org.jooq.util.jaxb.tools.XMLAppendable

plugins {
    id("nu.studer.jooq") version "10.0"
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    jooqGenerator("com.h2database:h2:2.3.232")
}

jooq {
    version.set("3.20.2")
    edition.set(JooqEdition.OSS)

    configurations {
        create("main") {
            jooqConfiguration {
                logging = Logging.WARN
                jdbc  {
                    driver = "org.h2.Driver"
                    url = "jdbc:h2:~/test;AUTO_SERVER=TRUE"
                    user = "sa"
                    password = ""
                    properties {
                        property {
                            key = "PAGE_SIZE"
                            value = "2048"
                        }
                        property {
                            key = "PAGE_SIZE"
                            value = "2048"
                        }
                    }
                }
                generator {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database {
                        name = "org.jooq.meta.h2.H2Database"
                        forcedTypes {
                            forcedType {
                                name = "varchar"
                                includeExpression = ".*"
                                includeTypes = "JSONB?"
                            }
                            forcedType {
                                name = "varchar"
                                includeExpression = ".*"
                                includeTypes = "INET"
                            }
                        }
                    }
                    generate {
                        isDeprecated = false
                        isRecords = false
                        isImmutablePojos = false
                        isFluentSetters = false
                    }
                    target {
                        packageName = "nu.studer.sample"
                        directory = "src/generated/jooq"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}

// allows to omit the apply() function when configuring the jOOQ configuration
operator fun <T: XMLAppendable> T.invoke(block: T.() -> Unit) = this.apply(block)

// allows to simplify the declaration of lists of jOOQ configuration elements, providing some examples
// (taken from https://github.com/jOOQ/jOOQ/blob/main/jOOQ-meta-kotlin/src/main/kotlin/org/jooq/meta/kotlin/Extensions.kt)
fun Jdbc.properties(block: MutableList<Property>.() -> Unit) {
    block(properties)
}

fun Database.properties(block: MutableList<Property>.() -> Unit) {
    block(properties)
}

fun MutableList<Property>.property(block: Property.() -> Unit) {
    val e = Property()
    block(e)
    add(e)
}

fun Database.forcedTypes(block: MutableList<ForcedType>.() -> Unit) {
    block(forcedTypes)
}

fun MutableList<ForcedType>.forcedType(block: ForcedType.() -> Unit) {
    val f = ForcedType()
    block(f)
    add(f)
}
