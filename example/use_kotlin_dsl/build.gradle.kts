import nu.studer.gradle.jooq.JooqEdition
import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Property

plugins {
    id("nu.studer.jooq") version "5.0.2"
    id("java")
}

repositories {
    jcenter()
}

dependencies {
    jooqGenerator("com.h2database:h2:1.4.200")
}

jooq {
    version.set("3.13.4")
    edition.set(JooqEdition.OSS)

    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = "org.h2.Driver"
                    url = "jdbc:h2:~/test;AUTO_SERVER=TRUE"
                    user = "sa"
                    password = ""
                    properties.add(Property().withKey("PAGE_SIZE").withValue("2048"))
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.h2.H2Database"
                        forcedTypes.addAll(arrayOf(
                            ForcedType()
                                .withName("varchar")
                                .withIncludeExpression(".*")
                                .withIncludeTypes("JSONB?"),
                            ForcedType()
                                .withName("varchar")
                                .withIncludeExpression(".*")
                                .withIncludeTypes("INET")
                        ).toList())
                    }
                    generate.apply {
                        isDeprecated = false
                        isRecords = false
                        isImmutablePojos = false
                        isFluentSetters = false
                    }
                    target.apply {
                        packageName = "nu.studer.sample"
                        directory = "src/generated/jooq"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}
