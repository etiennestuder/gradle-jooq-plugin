import nu.studer.gradle.jooq.JooqEdition

plugins {
    id("nu.studer.jooq")
    id("java-library")
}

repositories {
    jcenter()
}

dependencies {
    jooqRuntime("com.h2database:h2:1.4.193")
}

jooq {
    version = "3.13.2"
    edition = JooqEdition.OSS
    "sample"(sourceSets["main"]) {
        jdbc {
            driver = "org.h2.Driver"
            url = "jdbc:h2:~/test;AUTO_SERVER=TRUE"
            user = "sa"
            password = ""
        }
        generator {
            name = "org.jooq.codegen.DefaultGenerator"
            database {
                name = "org.jooq.meta.h2.H2Database"
                includes = ".*"
                excludes = ""
            }
            generate {
                isDeprecated = false
                isRecords = false
                isImmutablePojos = false
                isFluentSetters = false
            }
            target {
                packageName = "nu.studer.sample"
            }
            strategy {
                name = "org.jooq.codegen.DefaultGeneratorStrategy"
            }
        }
    }
}
