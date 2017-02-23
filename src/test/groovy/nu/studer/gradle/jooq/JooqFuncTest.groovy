package nu.studer.gradle.jooq

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import com.opentable.db.postgres.junit.SingleInstancePostgresRule
import groovy.sql.Sql
import org.gradle.testkit.runner.TaskOutcome
import org.junit.ClassRule
import spock.lang.AutoCleanup
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll

@Unroll
class JooqFuncTest extends BaseFuncTest {

    @ClassRule
    @Shared
    SingleInstancePostgresRule pgRule = EmbeddedPostgresRules.singleInstance()

    @AutoCleanup
    @Shared
    Sql sql

    void setupSpec() {
        sql = new Sql(pgRule.embeddedPostgres.postgresDatabase)
    }

    void setup() {
        sql.execute("CREATE SCHEMA IF NOT EXISTS public;")
    }

    void cleanup() {
        sql.execute('DROP SCHEMA public CASCADE;')
    }

    void "successfully applies jooq plugin"() {
        given:
        buildFile << buildWithJooqPluginDSL()
        sql.execute('CREATE TABLE public.foo (a INTEGER);')

        when:
        def result = runWithArguments('build')

        then:
        new File(workspaceDir, "build/generated-src/jooq/sample/nu/studer/sample/tables/Foo.java").exists()

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
    jooqRuntime 'postgresql:postgresql:9.1-901.jdbc4'
}

jooq {
   version = '3.9.0'
   edition = 'OSS'
   sample(sourceSets.main) {
       jdbc {
           driver = 'org.postgresql.Driver'
           url = 'jdbc:postgresql://localhost:${pgRule.embeddedPostgres.port}/postgres'
           user = 'postgres'
           password = ''
       }
       generator {
           name = 'org.jooq.util.DefaultGenerator'
           strategy {
               name = 'org.jooq.util.DefaultGeneratorStrategy'
           }
           database {
               name = 'org.jooq.util.postgres.PostgresDatabase'
               inputSchema = 'public'
           }
           generate {
               relations = true
               deprecated = false
               records = true
               immutablePojos = true
               fluentSetters = true
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
    jooqRuntime 'postgresql:postgresql:9.1-901.jdbc4'
}

jooq {
   version = '3.9.0'
   edition = 'OSS'
   sample(sourceSets.main) {
       jdbc {
           driver = 'org.postgresql.Driver'
           url = 'jdbc:postgresql://localhost:${pgRule.embeddedPostgres.port}/postgres'
           user = 'postgres'
           password = ''
       }
       generator {
           name = 'org.jooq.util.DefaultGenerator'
           strategy {
               name = 'org.jooq.util.DefaultGeneratorStrategy'
           }
           database {
               name = 'org.jooq.util.postgres.PostgresDatabase'
               inputSchema = 'public'
               forcedTypes = [
                    {
                        userType = 'java.time.LocalDateTime'
                        converter = 'com.company.dao.LocalDateTimeConverter'
                        expression = '.*\\\\.DATE_TIME.*'
                    }
               ]
           }
           generate {
               relations = true
               deprecated = false
               records = true
               immutablePojos = true
               fluentSetters = true
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
