plugins {
    id("nu.studer.jooq") version "3.0.3"
}

repositories {
    jcenter()
}

dependencies {
    implementation("org.jooq:jooq:3.11.11")
    jooqRuntime("com.h2database:h2:1.4.193")
}

jooq {
    version = "3.11.11"
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
