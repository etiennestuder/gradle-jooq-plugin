package nu.studer.gradle.jooq

import groovy.sql.Sql
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.AutoCleanup
import spock.lang.Ignore
import spock.lang.Shared

import java.sql.DriverManager

class JooqFuncTest extends BaseFuncTest {

    @AutoCleanup
    @Shared
    Sql sql

    void setupSpec() {
        sql = new Sql(DriverManager.getConnection('jdbc:h2:~/test;AUTO_SERVER=TRUE', 'sa', ''))
        sql.execute('CREATE SCHEMA IF NOT EXISTS jooq_test')
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

    @Ignore
    void "successfully applies DSL with forced types"() {
        given:
        buildFile << buildWithForcedTypes()

        when:
        def result = runWithArguments('build')

        then:
        result.task(':generateSampleJooqSchemaSource').outcome == TaskOutcome.SUCCESS
    }

    private String buildWithJooqPluginDSL(String targetPackageName = 'nu.studer.sample') {
        """
plugins {
    id 'nu.studer.jooq' version '2.0.2'
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
   version = '3.9.0'
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
           }
           generate {
           }
           target {
               packageName = '$targetPackageName'
           }
       }
   }
}
"""
    }

    private String buildWithForcedTypes() {
        """
plugins {
    id 'nu.studer.jooq' version '2.0.2'
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
   version = '3.9.0'
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
               forcedTypes = [
                   {
                       userType = 'java.time.LocalDateTime'
                       converter = 'com.company.dao.LocalDateTimeConverter'
                       expression = '.*\\\\.DATE_TIME.*'
                   }
              ]
           }
           generate {
           }
           target {
               packageName = '$targetPackageName'
           }
       }
   }
}
"""
    }
}
