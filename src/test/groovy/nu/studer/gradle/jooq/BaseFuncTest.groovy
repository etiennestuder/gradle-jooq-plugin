package nu.studer.gradle.jooq

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestName
import spock.lang.Shared
import spock.lang.Specification

import java.lang.management.ManagementFactory

abstract class BaseFuncTest extends Specification {

    @Shared
    File testKitDir

    void setupSpec() {
        // define the location of testkit, taking into account that multiple test workers might run in parallel
        testKitDir = new File("build/testkit").absoluteFile
        def workerNum = System.getProperty("org.gradle.test.worker")
        if (workerNum) {
            testKitDir = new File(testKitDir, workerNum)
        }
    }

    @Rule
    TemporaryFolder tempDir = new TemporaryFolder()

    @Rule
    TestName testName = new TestName()

    File workspaceDir
    GradleVersion gradleVersion

    void setup() {
        workspaceDir = new File(tempDir.root, testName.methodName)
        gradleVersion = determineGradleVersion()
    }

    protected BuildResult runWithArguments(String... args) {
        GradleRunner.create()
            .withPluginClasspath()
            .withTestKitDir(testKitDir)
            .withProjectDir(workspaceDir)
            .withArguments(args)
            .withGradleVersion(gradleVersion.version)
            .withDebug(isDebuggerAttached())
            .build()
    }

    protected BuildResult runAndFailWithArguments(String... args) {
        GradleRunner.create()
            .withPluginClasspath()
            .withTestKitDir(testKitDir)
            .withProjectDir(workspaceDir)
            .withArguments(args)
            .withGradleVersion(gradleVersion.version)
            .withDebug(isDebuggerAttached())
            .buildAndFail()
    }
    protected File getBuildFile() {
        file('build.gradle')
    }

    protected File file(String path) {
        file(workspaceDir, path)
    }

    protected File file(File dir, String path) {
        def file = new File(dir, path)
        assert file.parentFile.mkdirs() || file.parentFile.directory
        if (file.exists()) {
            assert file.file
        } else {
            assert file.createNewFile()
        }
        file
    }

    protected static GradleVersion determineGradleVersion() {
        def injectedGradleVersionString = System.getProperty('testContext.gradleVersion')
        injectedGradleVersionString ? GradleVersion.version(injectedGradleVersionString) : GradleVersion.current()
    }

    protected static boolean isDebuggerAttached() {
        ManagementFactory.runtimeMXBean.inputArguments.toString().indexOf("-agentlib:jdwp") > 0
    }

}
