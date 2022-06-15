plugins {
    id("nu.studer.jooq") version "7.1.1"
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    jooqGenerator("com.h2database:h2:2.1.214")
}

jooq {
   configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
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
                    target.apply {
                        packageName = "nu.studer.sample"
                    }
                }
            }
        }
    }
}

// by default, generateJooq will use the configured java toolchain, if any
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.named<nu.studer.gradle.jooq.JooqGenerate>("generateJooq") {
    // generateJooq can be configured to use a different/specific toolchain
    (launcher::set)(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(13))
    })
}
