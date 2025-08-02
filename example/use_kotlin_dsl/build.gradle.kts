import org.jooq.meta.kotlin.*
import nu.studer.gradle.jooq.JooqEdition

plugins {
    id("nu.studer.jooq") version "10.1.1"
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    jooqGenerator("com.h2database:h2:2.3.232")
}

jooq {
    version.set("3.20.5")
    edition.set(JooqEdition.OSS)

    configurations {
        create("main") {
            jooqConfiguration {
                logging = org.jooq.meta.jaxb.Logging.WARN
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
