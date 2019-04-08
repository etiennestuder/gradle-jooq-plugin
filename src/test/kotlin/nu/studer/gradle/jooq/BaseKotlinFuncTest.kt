package nu.studer.gradle.jooq

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestName
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.Files

abstract class BaseKotlinFuncTest {

    lateinit var testKitDir: File

    @Before
    open fun up() {
        // define the location of testkit, taking into account that multiple test workers might run in parallel
        testKitDir = File("build/testkit").absoluteFile
        val workerNum = System.getProperty("org.gradle.test.worker")
        if (workerNum != null) {
            testKitDir = File(testKitDir, workerNum)
        }
    }

    @get:Rule
    val tempDir = TemporaryFolder()

    @get:Rule
    val testName = TestName()

    lateinit var workspaceDir: File
    lateinit var gradleVersion: GradleVersion

    @Before
    fun setup() {
        workspaceDir = File(tempDir.root, testName.methodName)
        gradleVersion = determineGradleVersion()
    }

    protected fun runWithArguments(vararg  args: String): BuildResult = gradleRunner(*args).build()

    protected fun runAndFailWithArguments(vararg args: String) = gradleRunner(*args).buildAndFail()

    private fun gradleRunner(vararg args: String): GradleRunner  {
        Files.createDirectories(workspaceDir.toPath())
        return GradleRunner.create()
            .withPluginClasspath()
            .withTestKitDir(testKitDir)
            .withProjectDir(workspaceDir)
            .withArguments(*args)
            .forwardOutput()
            .withGradleVersion(gradleVersion.version)
            .withDebug(isDebuggerAttached())
    }

    protected open val buildFile: File get() = file("build.gradle")

    protected open val settingsFile: File get() = file("settings.gradle")

    fun dir(path: String): File {
        val file = File(workspaceDir, path)
        assert(file.parentFile.mkdirs() || file.parentFile.isDirectory)
        if (file.exists()) {
            assert(file.isDirectory)
        } else {
            assert(file.mkdir())
        }
        return file
    }

    fun file(path: String) = file(workspaceDir, path)

    private fun file(dir: File, path: String): File {
        val file = File(dir, path)
        assert(file.parentFile.mkdirs() || file.parentFile.isDirectory)
        if (file.exists()) {
            assert(file.isFile)
        } else {
            assert(file.createNewFile())
        }
        return file
    }

    private fun determineGradleVersion(): GradleVersion {
        val injectedGradleVersionString = System.getProperty("testContext.gradleVersion") ?: return GradleVersion.current()
        return GradleVersion.version(injectedGradleVersionString)
    }

    private fun isDebuggerAttached(): Boolean {
        return ManagementFactory.getRuntimeMXBean().inputArguments.toString().indexOf("-agentlib:jdwp") > 0
    }

}
