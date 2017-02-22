package nu.studer.gradle.jooq

import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

@Unroll
class JooqFuncTest extends BaseFuncTest {

    void "successfully applies jooq plugin"() {
        given:
        file('src/main/java/A.java') << someClass()

        buildFile << """
plugins {
    id 'nu.studer.jooq'
}
"""

        when:
        def result = runWithArguments('build')

        then:
        result.output.contains("BUILD SUCCESSFUL")
        result.task(':build').outcome == TaskOutcome.UP_TO_DATE
    }

    private static String someClass() {
        """
class A {}
"""
    }

}
