package nu.studer.gradle.jooq

import groovy.sql.Sql
import org.gradle.testkit.runner.TaskOutcome
import org.jooq.Constants
import spock.lang.AutoCleanup
import spock.lang.Shared

import java.sql.DriverManager

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

    void "successfully applies custom strategies when an external library is added to the jooqRuntime configuration"() {
        given:
        buildFile << buildWithCustomStrategiesOnExternalLibrary()

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
        def configXml = new File(workspaceDir, 'build/tmp/jooq/config.xml')
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

    void "can remove auto-wired task dependency"() {
        given:
        buildFile << buildWithJooqPluginDSL() << """
            project.tasks.getByName('compileJava').dependsOn -= 'generateSampleJooqSchemaSource'
        """

        when:
        def result = runWithArguments('build')

        then:
        !result.task(':generateSampleJooqSchemaSource')
    }

    void "can clean sources generated by jooq as part of the clean life-cycle task"() {
        given:
        buildFile << buildWithJooqPluginDSL('nu.studer.sample', 'src/generated/jooq/sample')

        when:
        runWithArguments('build')

        then:
        new File(workspaceDir, 'src/generated/jooq/sample/nu/studer/sample/jooq_test/tables/Foo.java').exists()

        when:
        def result = runWithArguments('clean')

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
    compile 'org.jooq:jooq'
    jooqRuntime 'com.h2database:h2:1.4.193'
}

jooq {
   version = '3.10.1'
   edition = 'OSS'
   sample(sourceSets.main) {
       jdbc {
           driver = 'org.h2.Driver'
           url = 'jdbc:h2:~/test;AUTO_SERVER=TRUE'
           user = 'sa'
           password = ''
       }
       generator {
           name = 'org.jooq.util.DefaultGenerator'
           strategy {
               name = 'org.jooq.util.DefaultGeneratorStrategy'
           }
           database {
               name = 'org.jooq.util.h2.H2Database'
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
               ${targetDirectory? "directory = '$targetDirectory'" : ""}
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
    compile 'org.jooq:jooq'
    jooqRuntime 'com.h2database:h2:1.4.193'
}

jooq {
   version = '3.10.1'
   edition = 'OSS'
   sample(sourceSets.main) {
       jdbc {
           driver = 'org.h2.Driver'
           url = 'jdbc:h2:~/test;AUTO_SERVER=TRUE'
           user = 'sa'
           password = ''
       }
       generator {
           name = 'org.jooq.util.DefaultGenerator'
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
    compile 'org.jooq:jooq'
    jooqRuntime 'com.h2database:h2:1.4.193'
}

jooq {
   version = '3.10.1'
   edition = 'OSS'
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

    private static String buildWithCustomStrategiesOnExternalLibrary() {
        """
plugins {
    id 'nu.studer.jooq'
}

apply plugin: 'java'

repositories {
    jcenter()
}

dependencies {
    compile 'org.jooq:jooq'
    jooqRuntime 'com.h2database:h2:1.4.193'
    jooqRuntime 'io.github.jklingsporn:vertx-jooq:1.0.0'
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
           name = 'org.jooq.util.DefaultGenerator'
           strategy {
               name = 'io.github.jklingsporn.vertx.impl.VertxGeneratorStrategy'
           }
           database {
               name = 'org.jooq.util.h2.H2Database'
           }
           generate {
               javaTimeTypes = true
           }
       }
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
    compile 'org.jooq:jooq'
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
           name = 'org.jooq.util.DefaultGenerator'
           strategy {
                name = 'nu.studer.sample.SampleGeneratorStrategy'  // use the custom generator strategy
           }
           database {
               name = 'org.jooq.util.h2.H2Database'
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
    compile("org.jooq:jooq-codegen:3.10.1")
}
"""
    }

    private static String sampleGeneratorStrategyClass() {
        """
package nu.studer.sample;

import org.jooq.util.DefaultGeneratorStrategy;
import org.jooq.util.Definition;

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
    compile 'org.jooq:jooq'
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
               name = 'org.jooq.util.h2.H2Database'
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
    compile 'org.jooq:jooq'
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
           name = 'org.jooq.util.DefaultGenerator'
           strategy {
               name = 'org.jooq.util.DefaultGeneratorStrategy'
           }
           database {
               name = 'org.jooq.util.h2.H2Database'
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

}
