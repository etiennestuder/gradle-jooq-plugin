package nu.studer.gradle.jooq

import groovy.sql.Sql
import org.gradle.testkit.runner.TaskOutcome
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.sql.DriverManager

class JooqKotlinFuncTest : BaseKotlinFuncTest() {

    lateinit var sql: Sql

    @Before
    override fun up() {
        super.up()
        sql = Sql(DriverManager.getConnection("jdbc:h2:~/test;AUTO_SERVER=TRUE", "sa", ""))
        sql.execute("CREATE SCHEMA IF NOT EXISTS jooq_test;")
        sql.execute("CREATE TABLE IF NOT EXISTS jooq_test.foo (a INT);")
    }

    @After
    fun cleanupSpec() {
        sql.execute("DROP SCHEMA jooq_test")
        sql.close()
    }

    @Test
    fun appliesJooqPluginTest() {
        buildFile.writeText(buildWithJooqPluginDSL())
        val result = runWithArguments("build")
        assert(File(workspaceDir, "build/generated-src/jooq/sample/nu/studer/sample/jooq_test/tables/Foo.java").exists())
        assert(result.task(":generateSampleJooqSchemaSource") != null)

    }

    @Test
    fun participatesInUpToDateChecksConfigSameTest() {
        buildFile.writeText(buildWithJooqPluginDSL())
        runWithArguments("build")
        val result = runWithArguments("build")
        assert(result.task(":generateSampleJooqSchemaSource")?.outcome == TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun participatesInUpToDateChecksConfigDifferent() {
        buildFile.writeText(buildWithJooqPluginDSL())
        runWithArguments("build")
        buildFile.delete()
        buildFile.writeText(buildWithJooqPluginDSL("different.target.package.name"))
        val result = runWithArguments("build")
        assert(result.task(":generateSampleJooqSchemaSource")?.outcome == TaskOutcome.SUCCESS)
    }

    @Test
    fun showsAnErrorMessageWhenSettingUnsupportedPropertyTest() {
        buildFile.writeText(buildWithMissingProperty())
        val result = runAndFailWithArguments("build")
        assert(result.output.contains(Regex.fromLiteral("Unresolved reference: missing")))
    }

    @Test
    fun appliesCustomStrategiesWhenSubProjectIsAddedToJooqRuntimeConfigTest() {
        buildFile.writeText(buildWithCustomStrategiesOnSubProject())
        createCustomGeneratorSubProject()
        settingsFile.appendText("""include("custom-generator")""")
        val result = runWithArguments("build")
        assert(result.task(":generateSampleJooqSchemaSource")?.outcome == TaskOutcome.SUCCESS)
    }

    @Test
    fun acceptsMatcherStrategiesWhenNameIsExplicitlyNull() {
        buildFile.writeText(buildWithMatcherStrategies())
        val result = runWithArguments("build")
        assert(result.task(":generateSampleJooqSchemaSource")?.outcome == TaskOutcome.SUCCESS)
    }

    @Test
    fun parsesDSLWithVariablesAndMethodsReferences() {
        buildFile.writeText(buildWithVariablesAndMethods())
        val result = runWithArguments("build")
        assert(result.task(":generateSampleJooqSchemaSource")?.outcome == TaskOutcome.SUCCESS)
    }

    @Test
    fun canRemoveAutoWiredTaskDependency() {
        buildFile.run {
            writeText(buildWithJooqPluginDSL())
            appendText(
                """

                    project.tasks.getByName("compileJava").dependsOn -= "generateSampleJooqSchemaSource"
                """.trimIndent()
            )
        }
        val  result = runWithArguments("build")
        assert(result.task(":generateSampleJooqSchemaSource") == null)
    }

    @Test
    fun canCleanSourcesGeneratedByJooqByCallingCleanTaskRule() {
        buildFile.writeText(buildWithJooqPluginDSL("nu.studer.sample", "src/generated/jooq/sample"))
        runWithArguments("build")
        assert(File(workspaceDir, "src/generated/jooq/sample/nu/studer/sample/jooq_test/tables/Foo.java").exists())
        val result = runWithArguments("cleanGenerateSampleJooqSchemaSource")
        assert(File(workspaceDir, "src/generated/jooq/sample/nu/studer/sample/jooq_test/tables/Foo.java").exists().not())
        assert(result.task(":cleanGenerateSampleJooqSchemaSource")?.outcome == TaskOutcome.SUCCESS)
    }

    override val buildFile: File get() = file("build.gradle.kts")
    override val settingsFile: File get() = file("settings.gradle.kts")

    private fun createCustomGeneratorSubProject() {
        dir("custom-generator/src/main/java/java/nu/studer/sample")
        val buildFile = file("custom-generator/build.gradle.kts")
        buildFile.writeText(customGeneratorBuildFile())
        val strategy = file("custom-generator/src/main/java/java/nu/studer/sample/SampleGeneratorStrategy.java")
        strategy.writeText(sampleGeneratorStrategy())

    }

    private fun buildWithVariablesAndMethods() =
        """
            import nu.struder.gradle.jooq.*
            import nu.studer.gradle.jooq.JooqEdition
            import org.jooq.meta.jaxb.MatcherTransformType

            plugins {
                java
                id("nu.studer.jooq")
            }

            repositories {
                jcenter()
            }

            dependencies {
                compile("org.jooq:jooq")
                jooqRuntime("com.h2database:h2:1.4.193")
            }

            val userSA = "sa"

            fun calculateDriver() = "org.h2.Driver"

            jooq {
               "sample"(sourceSets["main"]) {
                   jdbc {
                       driver = calculateDriver()
                       url = "jdbc:h2:~/test;AUTO_SERVER=TRUE"
                       user = userSA
                       password = ""
                   }
                   generator {
                       name = "org.jooq.codegen.DefaultGenerator"
                       strategy {
                           name = "org.jooq.codegen.DefaultGeneratorStrategy"
                       }
                       database {
                           name = "org.jooq.meta.h2.H2Database"
                           includes = ".*"
                           excludes = ""
                       }
                       generate {
                           isJavaTimeTypes = true
                       }
                   }
               }
            }
        """.trimIndent()


    private fun buildWithMatcherStrategies() =
        """
            import nu.struder.gradle.jooq.*
            import nu.studer.gradle.jooq.JooqEdition
            import org.jooq.meta.jaxb.MatcherTransformType

            plugins {
                java
                id("nu.studer.jooq")
            }

            repositories {
                jcenter()
            }

            dependencies {
                compile("org.jooq:jooq")
                jooqRuntime("com.h2database:h2:1.4.193")
            }

            jooq {
               "sample"(sourceSets["main"]) {
                   jdbc {
                       driver = "org.h2.Driver"
                       url = "jdbc:h2:~/test;AUTO_SERVER=TRUE"
                       user = "sa"
                       password = ""
                   }
                   generator {
                        strategy {
                            name = null
                            matchers {
                              tables {
                                table {
                                    pojoClass {
                                        transform = MatcherTransformType.PASCAL
                                        expression = "\$0_POJO"
                                    }
                                }
                              }
                            }
                        }
                        database {
                           name = "org.jooq.meta.h2.H2Database"
                        }
                        generate {
                           isJavaTimeTypes = true
                        }
                    }
               }
            }
        """


    private fun sampleGeneratorStrategy()  =
        """
            package nu.studer.sample;

            import org.jooq.codegen.DefaultGeneratorStrategy;
            import org.jooq.meta.Definition;

            public final class SampleGeneratorStrategy extends DefaultGeneratorStrategy {

                @Override
                public String getJavaGetterName(Definition definition, Mode mode) {
                    // do not prefix getters with 'get'
                    return super.getJavaGetterName(definition, mode).substring("get".length());
                }

            }
        """

    private fun customGeneratorBuildFile()  =
        """
            plugins {
                java
            }

            repositories {
                jcenter()
            }

            dependencies {
                compile("org.jooq:jooq-codegen:3.11.2")
            }

        """.trimIndent()

    private fun buildWithCustomStrategiesOnSubProject() =
        """
            import nu.struder.gradle.jooq.*
            import nu.studer.gradle.jooq.JooqEdition

            plugins {
                java
                id("nu.studer.jooq")
            }

            repositories {
                jcenter()
            }

            dependencies {
                compile("org.jooq:jooq")
                jooqRuntime("com.h2database:h2:1.4.193")
                jooqRuntime(project(":custom-generator"))  // include sub-project that contains the custom generator strategy
            }

            jooq {
               "sample"(sourceSets["main"]) {
                   jdbc {
                       driver = "org.h2.Driver"
                       url = "jdbc:h2:~/test;AUTO_SERVER=TRUE"
                       user = "sa"
                       password = ""
                   }
                   generator {
                       name = "org.jooq.codegen.DefaultGenerator"
                       strategy {
                           name = "nu.studer.sample.SampleGeneratorStrategy"
                       }
                       database {
                           name = "org.jooq.meta.h2.H2Database"
                       }
                       generate {
                           isJavaTimeTypes = true
                       }
                   }
               }
            }
        """.trimIndent()

    private fun buildWithMissingProperty() =
        """
            import nu.struder.gradle.jooq.*
            import nu.studer.gradle.jooq.JooqEdition

            plugins {
                java
                id("nu.studer.jooq")
            }

            repositories {
                jcenter()
            }

            dependencies {
                compile("org.jooq:jooq")
                jooqRuntime("com.h2database:h2:1.4.193")
            }

            jooq {
               version = "3.11.2"
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
                       generate {
                           missing = true
                       }
                   }
               }
            }
            """.trimIndent()

    private fun buildWithJooqPluginDSL(targetPackageName: String = "nu.studer.sample", targetDirectory: String? = null) =
        """
            import nu.struder.gradle.jooq.*
            import nu.studer.gradle.jooq.JooqEdition

            plugins {
                java
                id("nu.studer.jooq")
            }

            repositories {
                jcenter()
            }

            dependencies {
                compile("org.jooq:jooq")
                jooqRuntime("com.h2database:h2:1.4.193")
            }

            jooq {
               version = "3.11.2"
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
                       strategy {
                           name = "org.jooq.codegen.DefaultGeneratorStrategy"
                       }
                       database {
                           name = "org.jooq.meta.h2.H2Database"
                           includes = ".*"
                           excludes = ""
                           forcedTypes {
                             forcedType {
                                 name = "varchar"
                                 expression = ".*"
                                 types = "JSONB?"
                             }
                             forcedType {
                                 name = "varchar"
                                 expression = ".*"
                                 types = "INET"
                             }
                           }
                       }
                       generate {
                           isJavaTimeTypes = true
                       }
                       target {
                           packageName = "$targetPackageName"
                           ${if (targetDirectory != null) "directory = \"$targetDirectory\"" else ""}
                       }
                   }
               }
            }
            """.trimIndent()
}