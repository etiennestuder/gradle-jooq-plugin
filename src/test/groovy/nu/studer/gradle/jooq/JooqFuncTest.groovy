package nu.studer.gradle.jooq

import groovy.sql.Sql
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jooq.Constants
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Unroll

import java.sql.DriverManager

@Unroll
class JooqFuncTest extends BaseFuncTest {

    @AutoCleanup
    @Shared
    Sql sql

    void setupSpec() {
        sql = new Sql(DriverManager.getConnection('jdbc:h2:~/test;AUTO_SERVER=TRUE', 'sa', ''))
        sql.execute('CREATE SCHEMA IF NOT EXISTS jooq_test;')
        sql.execute('CREATE TABLE IF NOT EXISTS jooq_test.foo (a INT);')
    }

    void cleanupSpec() {
        sql.execute('DROP TABLE jooq_test.foo')
        sql.execute('DROP SCHEMA jooq_test')
    }

    void "successfully applies jooq plugin"() {
        given:
        buildFile << buildWithJooqPluginDSL()

        when:
        def result = runWithArguments('build')

        then:
        new File(workspaceDir, 'build/generated-src/jooq/sample/nu/studer/sample/jooq_test/tables/Foo.java').exists()

        and:
        result.task(':generateSampleJooqSchemaSource')
    }

    void "successfully applies jooq plugin with Gradle configuration cache enabled"() {
        given:
        gradleVersion = GradleVersion.version('6.5')
        buildFile << buildWithJooqPluginDSL()

        when:
        def result = runWithArguments('build', '--configuration-cache=on')

        then:
        new File(workspaceDir, 'build/generated-src/jooq/sample/nu/studer/sample/jooq_test/tables/Foo.java').exists()
        result.output.contains("Calculating task graph as no configuration cache is available for tasks: build")
        result.task(':generateSampleJooqSchemaSource').outcome == TaskOutcome.SUCCESS

        when:
        result = runWithArguments('build', '--configuration-cache=on')

        then:
        result.output.contains("Reusing configuration cache.")
        result.task(':generateSampleJooqSchemaSource').outcome == TaskOutcome.UP_TO_DATE
    }

    void "participates in the up-to-date checks when the configuration is the same"() {
        given:
        buildFile << buildWithJooqPluginDSL()
        runWithArguments('build')

        when:
        def result = runWithArguments('build')

        then:
        result.task(':generateSampleJooqSchemaSource').outcome == TaskOutcome.UP_TO_DATE
    }

    void "participates in the up-to-date checks when the configuration is different"() {
        given:
        buildFile << buildWithJooqPluginDSL()
        runWithArguments('build')

        buildFile.delete()
        buildFile << buildWithJooqPluginDSL("different.target.package.name")

        when:
        def result = runWithArguments('build')

        then:
        result.task(':generateSampleJooqSchemaSource').outcome == TaskOutcome.SUCCESS
    }

    void "participates in the up-to-date checks when parts of the outputs are removed"() {
        given:
        buildFile << buildWithJooqPluginDSL()
        runWithArguments('build')

        new File(workspaceDir, 'build/generated-src/jooq/sample/nu/studer/sample/jooq_test/tables/Foo.java').delete()

        when:
        def result = runWithArguments('build')

        then:
        result.task(':generateSampleJooqSchemaSource').outcome == TaskOutcome.SUCCESS
    }

    void "shows an error message with a link to the current XSD when a property is missing"() {
        given:
        buildFile << buildWithMissingProperty()

        when:
        def result = runAndFailWithArguments('build')

        then:
        result.output.contains "Invalid property: 'missing' on extension 'jooq.sample.generator.generate', value: true. Please check the current XSD: https://www.jooq.org/xsd/${Constants.XSD_CODEGEN}"
    }

    void "shows an error message with a link to the current XSD when a configuration container element is missing"() {
        given:
        buildFile << buildWithMissingConfigurationContainerElement()

        when:
        def result = runAndFailWithArguments('build')

        then:
        result.output.contains "Invalid configuration container element: 'missing' on extension 'jooq.sample'. Please check the current XSD: https://www.jooq.org/xsd/${Constants.XSD_CODEGEN}"
    }

    void "successfully applies custom strategies when a sub project is added to the jooqRuntime configuration"() {
        given:
        buildFile << buildWithCustomStrategiesOnSubProject()
        createCustomGeneratorSubProject()
        settingsFile << "include 'custom-generator'"

        when:
        def result = runWithArguments('build')

        then:
        result.task(':generateSampleJooqSchemaSource').outcome == TaskOutcome.SUCCESS
    }

    void "correctly writes out boolean default values"() {
        given:
        buildFile << buildWithJooqPluginDSL()

        when:
        def result = runWithArguments('build')

        then:
        def configXml = new File(workspaceDir, 'build/tmp/generateSampleJooqSchemaSource/config.xml')
        configXml.exists()

        def rootNode = new XmlSlurper().parse(configXml)
        rootNode.generator.generate.globalTableReferences == true
        rootNode.generator.generate.emptySchemas == false

        and:
        result.task(':generateSampleJooqSchemaSource')
    }

    void "successfully applies custom strategies when a submodule is added to the jooqRuntime configuration"() {
        given:
        buildFile << buildWithCustomStrategiesOnSubmodule()
        file('settings.gradle') << "include 'custom-generator'"
        file('custom-generator/build.gradle') << sampleGeneratorStrategyBuild()
        file('custom-generator/src/main/java/nu/studer/sample/SampleGeneratorStrategy.java') << sampleGeneratorStrategyClass()

        when:
        def result = runWithArguments('build')

        then:
        result.task(':generateSampleJooqSchemaSource').outcome == TaskOutcome.SUCCESS

        and:
        def foo = new File(workspaceDir, 'build/generated-src/jooq/sample/org/jooq/generated/jooq_test/tables/records/FooRecord.java')
        foo.exists()
        foo.text.contains("public Integer A() {") // instead of getA, as the sample generator strategy removed the get prefix
    }

    void "accepts matcher strategies when name is explicitly set to null"() {
        given:
        buildFile << buildWithMatcherStrategies()

        when:
        def result = runWithArguments('build')

        then:
        result.task(':generateSampleJooqSchemaSource').outcome == TaskOutcome.SUCCESS
    }

    void "parses DSL with variables and methods references"() {
        given:
        buildFile << buildWithVariablesAndMethods()

        when:
        def result = runWithArguments('build')

        then:
        result.task(':generateSampleJooqSchemaSource').outcome == TaskOutcome.SUCCESS
    }

    void "can disable auto-generation of schema source on compilation (generateSchemaSourceOnCompilation #generateSchemaSourceOnCompilation)"() {
        given:
        buildFile.delete()
        buildFile << buildWithAutoGenerationOfSchemaSourceOnCompilationEnabled(generateSchemaSourceOnCompilation)

        when:
        def result = runWithArguments('build')

        then:
        (result.task(':generateSampleJooqSchemaSource') != null) == generateSchemaSourceOnCompilation

        where:
        generateSchemaSourceOnCompilation << [true, false]
    }

    void "can clean sources generated by jooq by calling its clean task rule"() {
        given:
        buildFile << buildWithJooqPluginDSL('nu.studer.sample', 'src/generated/jooq/sample')

        when:
        runWithArguments('build')

        then:
        new File(workspaceDir, 'src/generated/jooq/sample/nu/studer/sample/jooq_test/tables/Foo.java').exists()

        when:
        def result = runWithArguments('cleanGenerateSampleJooqSchemaSource')

        then:
        !new File(workspaceDir, 'src/generated/jooq/sample/nu/studer/sample/jooq_test/tables/Foo.java').exists()
        result.task(':cleanGenerateSampleJooqSchemaSource').outcome == TaskOutcome.SUCCESS
    }

    private static String buildWithJooqPluginDSL(String targetPackageName = 'nu.studer.sample', String targetDirectory = null) {
        """
plugins {
    id 'nu.studer.jooq'
}

apply plugin: 'java'

repositories {
    jcenter()
}

dependencies {
    implementation 'org.jooq:jooq'
    jooqRuntime 'com.h2database:h2:1.4.193'
}

jooqVersion = '3.13.2'
jooqEdition = 'OSS'

jooq {
   sample(sourceSets.main) {
       jdbc {
           driver = 'org.h2.Driver'
           url = 'jdbc:h2:~/test;AUTO_SERVER=TRUE'
           user = 'sa'
           password = ''
       }
       generator {
           name = 'org.jooq.codegen.DefaultGenerator'
           strategy {
               name = 'org.jooq.codegen.DefaultGeneratorStrategy'
           }
           database {
               name = 'org.jooq.meta.h2.H2Database'
               includes = '.*'
               excludes = ''
               forcedTypes {
                 forcedType {
                     name = 'varchar'
                     expression = '.*'
                     types = 'JSONB?'
                 }
                 forcedType {
                     name = 'varchar'
                     expression = '.*'
                     types = 'INET'
                 }
               }
           }
           generate {
               javaTimeTypes = true
           }
           target {
               packageName = '$targetPackageName'
               ${targetDirectory ? "directory = '$targetDirectory'" : ""}
           }
       }
   }
}
"""
    }

    private static String buildWithMissingProperty() {
        """
plugins {
    id 'nu.studer.jooq'
}

apply plugin: 'java'

repositories {
    jcenter()
}

dependencies {
    implementation 'org.jooq:jooq'
    jooqRuntime 'com.h2database:h2:1.4.193'
}

jooqVersion = '3.13.2'
jooqEdition = 'OSS'

jooq {
   sample(sourceSets.main) {
       jdbc {
           driver = 'org.h2.Driver'
           url = 'jdbc:h2:~/test;AUTO_SERVER=TRUE'
           user = 'sa'
           password = ''
       }
       generator {
           name = 'org.jooq.codegen.DefaultGenerator'
           generate {
               missing = true
           }
       }
   }
}
"""
    }

    private static String buildWithMissingConfigurationContainerElement() {
        """
plugins {
    id 'nu.studer.jooq'
}

apply plugin: 'java'

repositories {
    jcenter()
}

dependencies {
    implementation 'org.jooq:jooq'
    jooqRuntime 'com.h2database:h2:1.4.193'
}

jooqVersion = '3.13.2'
jooqEdition = 'OSS'

jooq {
   sample(sourceSets.main) {
       jdbc {
           driver = 'org.h2.Driver'
           url = 'jdbc:h2:~/test;AUTO_SERVER=TRUE'
           user = 'sa'
           password = ''
       }
       missing {
       }
   }
}
"""
    }

    private static String buildWithCustomStrategiesOnSubProject() {
        """
plugins {
    id 'nu.studer.jooq'
}

apply plugin: 'java'

repositories {
    jcenter()
}

dependencies {
    implementation 'org.jooq:jooq'
    jooqRuntime 'com.h2database:h2:1.4.193'
    jooqRuntime project(':custom-generator')  // include sub-project that contains the custom generator strategy
}

jooq {
   sample(sourceSets.main) {
       jdbc {
           driver = 'org.h2.Driver'
           url = 'jdbc:h2:~/test;AUTO_SERVER=TRUE'
           user = 'sa'
           password = ''
       }
       generator {
           name = 'org.jooq.codegen.DefaultGenerator'
           strategy {
               name = 'nu.studer.sample.SampleGeneratorStrategy'
           }
           database {
               name = 'org.jooq.meta.h2.H2Database'
           }
           generate {
               javaTimeTypes = true
           }
       }
   }
}
"""
    }

    def createCustomGeneratorSubProject() {
        dir("custom-generator/src/main/java/java/nu/studer/sample")
        def buildFile = file("custom-generator/build.gradle")
        buildFile << customGeneratorBuildFile()
        def strategy = file("custom-generator/src/main/java/java/nu/studer/sample/SampleGeneratorStrategy.java")
        strategy << sampleGeneratorStrategy()

    }

    def customGeneratorBuildFile() {
        """apply plugin: 'java'

repositories {
    jcenter()
}

dependencies {
    implementation 'org.jooq:jooq-codegen:3.13.2'
}

"""
    }

    def sampleGeneratorStrategy() {
        """package nu.studer.sample;

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
    }

    private static String buildWithCustomStrategiesOnSubmodule() {
        """
plugins {
    id 'nu.studer.jooq'
}

apply plugin: 'java'

repositories {
    jcenter()
}

dependencies {
    implementation 'org.jooq:jooq'
    jooqRuntime 'com.h2database:h2:1.4.193'
    jooqRuntime project(':custom-generator')  // include sub-project that contains the custom generator strategy
}

jooq {
   sample(sourceSets.main) {
       jdbc {
           driver = 'org.h2.Driver'
           url = 'jdbc:h2:~/test;AUTO_SERVER=TRUE'
           user = 'sa'
           password = ''
       }
       generator {
           name = 'org.jooq.codegen.DefaultGenerator'
           strategy {
                name = 'nu.studer.sample.SampleGeneratorStrategy'  // use the custom generator strategy
           }
           database {
               name = 'org.jooq.meta.h2.H2Database'
               includes = '.*'
               excludes = ''
           }
           generate {
               javaTimeTypes = true
           }
       }
   }
}
"""
    }

    private static String sampleGeneratorStrategyBuild() {
        """
apply plugin: 'java'

repositories {
    jcenter()
}

dependencies {
    implementation 'org.jooq:jooq-codegen:3.13.2'
}
"""
    }

    private static String sampleGeneratorStrategyClass() {
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
    }

    private static String buildWithMatcherStrategies() {
        """
plugins {
    id 'nu.studer.jooq'
}

apply plugin: 'java'

repositories {
    jcenter()
}

dependencies {
    implementation 'org.jooq:jooq'
    jooqRuntime 'com.h2database:h2:1.4.193'
}

jooq {
   sample(sourceSets.main) {
       jdbc {
           driver = 'org.h2.Driver'
           url = 'jdbc:h2:~/test;AUTO_SERVER=TRUE'
           user = 'sa'
           password = ''
       }
       generator {
            strategy {
                name = null
                matchers {
                  tables {
                    table {
                        pojoClass {
                            transform = 'PASCAL'
                            expression = '\$0_POJO'
                        }
                    }
                  }
                }
            }
            database {
               name = 'org.jooq.meta.h2.H2Database'
            }
            generate {
               javaTimeTypes = true
            }
        }
   }
}
"""
    }

    private static String buildWithVariablesAndMethods() {
        """
plugins {
    id 'nu.studer.jooq'
}

apply plugin: 'java'

repositories {
    jcenter()
}

dependencies {
    implementation 'org.jooq:jooq'
    jooqRuntime 'com.h2database:h2:1.4.193'
}

def userSA = 'sa'

def calculateDriver() {
    'org.h2.Driver'
}

jooq {
   sample(sourceSets.main) {
       jdbc {
           driver = calculateDriver()
           url = 'jdbc:h2:~/test;AUTO_SERVER=TRUE'
           user = userSA
           password = ''
       }
       generator {
           name = 'org.jooq.codegen.DefaultGenerator'
           strategy {
               name = 'org.jooq.codegen.DefaultGeneratorStrategy'
           }
           database {
               name = 'org.jooq.meta.h2.H2Database'
               includes = '.*'
               excludes = ''
           }
           generate {
               javaTimeTypes = true
           }
       }
   }
}
"""
    }

    private static String buildWithAutoGenerationOfSchemaSourceOnCompilationEnabled(boolean generateSchemaSourceOnCompilation) {
        """
plugins {
    id 'nu.studer.jooq'
}

apply plugin: 'java'

repositories {
    jcenter()
}

dependencies {
    implementation 'org.jooq:jooq'
    jooqRuntime 'com.h2database:h2:1.4.193'
}

jooqVersion = '3.13.2'
jooqEdition = 'OSS'

jooq {
   generateSchemaSourceOnCompilation = ${generateSchemaSourceOnCompilation}
   sample(sourceSets.main) {
       jdbc {
           driver = 'org.h2.Driver'
           url = 'jdbc:h2:~/test;AUTO_SERVER=TRUE'
           user = 'sa'
           password = ''
       }
       generator {
           name = 'org.jooq.codegen.DefaultGenerator'
           database {
               name = 'org.jooq.meta.h2.H2Database'
               includes = '.*'
               excludes = ''
           }
           target {
               packageName = 'nu.studer.sample'
           }
       }
   }
}
"""
    }

}
