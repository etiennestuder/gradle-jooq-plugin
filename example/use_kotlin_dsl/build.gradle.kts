import nu.studer.gradle.jooq.JooqEdition

plugins {
    id("nu.studer.jooq") version "5.0.1"
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
                logging = org.jooq.meta.jaxb.Logging.INFO
                jdbc.apply {
                    driver = "org.h2.Driver"
                    url = "jdbc:h2:~/test;AUTO_SERVER=TRUE"
                    user = "sa"
                    password = ""
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.h2.H2Database"
                        includes = ".*"
                        excludes = ""
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
