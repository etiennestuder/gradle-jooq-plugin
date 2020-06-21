/**
 Copyright 2014 Etienne Studer

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package nu.studer.gradle.jooq

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.jooq.Constants
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration

import javax.inject.Inject
import javax.xml.XMLConstants
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory

/**
 * Gradle Task that runs the jOOQ source code generation.
 */
class JooqTask extends DefaultTask {

    @Internal
    Action<? super JavaExecSpec> javaExecSpec

    @Internal
    Action<? super ExecResult> execResultHandler

    @Classpath
    FileCollection jooqClasspath

    @Internal
    Configuration configuration

    private Configuration normalizedConfiguration

    private final ObjectFactory objects
    private final ProjectLayout projectLayout
    private final ExecOperations execOperations

    @Inject
    JooqTask(ObjectFactory objects, ProjectLayout projectLayout, ExecOperations execOperations) {
        this.objects = objects
        this.projectLayout = projectLayout
        this.execOperations = execOperations
    }

    @Input
    @SuppressWarnings("GroovyUnusedDeclaration")
    Configuration getNormalizedConfiguration() {
        if (normalizedConfiguration == null) {
            normalizedConfiguration = relativizeTo(configuration, projectLayout.projectDirectory.asFile)
        }
        normalizedConfiguration
    }

    private static Configuration relativizeTo(Configuration configuration, File dir) {
        def directoryValue = configuration.generator.target.directory
        if (directoryValue != null) {
            def file = new File(directoryValue)
            if (file.absolute) {
                String relativized = dir.toURI().relativize(file.toURI()).path
                if (relativized.endsWith(File.separator)) {
                    relativized = relativized[0..-2]
                }
                configuration.generator.target.directory = relativized
            }
        }
        configuration
    }

    @OutputDirectory
    @SuppressWarnings("GroovyUnusedDeclaration")
    File getOutputDirectory() {
        new File(projectLayout.projectDirectory.asFile, configuration.generator.target.directory)
    }

    @TaskAction
    @SuppressWarnings("GroovyUnusedDeclaration")
    void generate() {
        def configFile = new File(temporaryDir, "config.xml")
        def execResult = executeJooq(configFile, projectLayout)
        if (execResultHandler) {
            execResultHandler.execute(execResult)
        }
    }

    private ExecResult executeJooq(File configFile, ProjectLayout projectLayout) {
        execOperations.javaexec(new Action<JavaExecSpec>() {

            @Override
            void execute(JavaExecSpec spec) {
                spec.main = "org.jooq.codegen.GenerationTool"
                spec.classpath = jooqClasspath
                spec.workingDir = projectLayout.projectDirectory
                spec.args configFile

                if (javaExecSpec) {
                    javaExecSpec.execute(spec)
                }

                configFile.parentFile.mkdirs()
                writeConfiguration(getNormalizedConfiguration(), configFile)
            }

            private static byte[] writeConfiguration(Configuration config, File file) {
                SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                Schema schema = sf.newSchema(GenerationTool.class.getResource("/xsd/" + Constants.XSD_CODEGEN))

                JAXBContext ctx = JAXBContext.newInstance(Configuration.class)
                Marshaller marshaller = ctx.createMarshaller()
                marshaller.setSchema(schema)

                new FileOutputStream(file).withCloseable { fs -> marshaller.marshal(config, fs) }
            }

        })
    }

}
