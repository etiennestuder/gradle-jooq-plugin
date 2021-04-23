import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    kotlin("kapt") version "1.4.32"
    id("nu.studer.jooq") version "5.2.1"
}

repositories {
    mavenCentral()
}

dependencies {
    jooqGenerator("com.h2database:h2:1.4.200")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

jooq {
    version.set("3.14.8")

    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = "org.h2.Driver"
                    url = "jdbc:h2:~/test;AUTO_SERVER=TRUE"
                    user = "sa"
                    password = ""                }
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    target.apply {
                        packageName = "org.bug"
                    }
                }
            }
        }
    }
}

dependencies {
    implementation("org.mapstruct:mapstruct:1.4.2.Final")
    jooqGenerator("org.postgresql:postgresql:42.2.19")
    kapt("org.mapstruct:mapstruct-processor:1.4.2.Final")
}

tasks.named<nu.studer.gradle.jooq.JooqGenerate>("generateJooq") {
    allInputsDeclared.set(true)
    outputs.cacheIf { true }
}
