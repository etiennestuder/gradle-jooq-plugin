import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    id("nu.studer.jooq") version "8.1"
    id("org.liquibase.gradle") version "2.0.4"
}

val h2Url = "jdbc:h2:~/test"
val h2Driver = "org.h2.Driver"
val h2Username = "sa"
val h2Password = ""

repositories {
    mavenCentral()
}

dependencies {
    jooqGenerator("com.h2database:h2:2.1.214")
    compileOnly("org.liquibase:liquibase-core:4.18.0")
    liquibaseRuntime("org.liquibase:liquibase-core:4.2.2")
    liquibaseRuntime("org.liquibase:liquibase-groovy-dsl:2.1.1")
    liquibaseRuntime("com.h2database:h2:2.1.214")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

jooq {
    version.set("3.17.6")
    edition.set(nu.studer.gradle.jooq.JooqEdition.OSS)

    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = h2Driver
                    url = h2Url
                    user = h2Username
                    password = h2Password
                }

                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.h2.H2Database"
                        inputSchema = "PUBLIC"
                        includes = ".*"
                        excludes = ""
                    }
                }
            }
        }
    }
}

liquibase {
    activities.register("main") {
        this.arguments = mapOf(
            "logLevel" to "info",
            "changeLogFile" to "src/main/resources/db/schema-changelog.xml",
            "url" to h2Url,
            "username" to h2Username,
            "password" to h2Password,
            "driver" to h2Driver
        )
    }
    runList = "main"
}

tasks.named("generateJooq") {
    dependsOn(tasks.named("update"))
}