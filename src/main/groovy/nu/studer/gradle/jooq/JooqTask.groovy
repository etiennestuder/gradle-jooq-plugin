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
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.jooq.Constants
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration

import javax.xml.XMLConstants
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory

/**
 * Gradle Task that runs the jOOQ source code generation.
 */
@ParallelizableTask
class JooqTask extends DefaultTask {

    @Internal
    Action<? super JavaExecSpec> javaExecSpec

    @Internal
    Action<? super ExecResult> execResultHandler

    @InputFiles
    @Classpath
    FileCollection jooqClasspath

    @Internal
    Configuration configuration

    @Internal
    private byte[] configurationBytes

    @Input
    @SuppressWarnings("GroovyUnusedDeclaration")
    Configuration getConfigurationHash() {
        relativizeTo(configuration, project.projectDir)
    }

    @Internal
    private byte[] getConfigurationBytes() {
        if (configurationBytes == null) {
            def configurationWithOutputDirRelativeToProjectDir = relativizeTo(configuration, project.projectDir)
            configurationBytes = generateConfigurationBytes(configurationWithOutputDirRelativeToProjectDir)
        }

        configurationBytes
    }

    static Configuration relativizeTo(Configuration configuration, File dir) {
        def directoryValue = configuration.generator.target.directory
        if (directoryValue == null) {
            configuration
        } else {
            def file = new File(directoryValue)
            if (file.absolute) {
                String relativized = dir.toURI().relativize(file.toURI()).path
                if (relativized.endsWith(File.separator)) {
                    relativized = relativized[0..-2]
                }
                configuration.withGenerator(
                    configuration.generator.withTarget(
                        configuration.generator.target.withDirectory(relativized)
                    )
                )
            } else {
                configuration
            }
        }
    }

    private static byte[] generateConfigurationBytes(Configuration configuration) {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        Schema schema = sf.newSchema(GenerationTool.class.getResource("/xsd/" + Constants.XSD_CODEGEN))

        JAXBContext ctx = JAXBContext.newInstance(Configuration.class)
        Marshaller marshaller = ctx.createMarshaller()
        marshaller.setSchema(schema)

        def byteStream = new ByteArrayOutputStream()
        marshaller.marshal(configuration, byteStream)
        byteStream.toByteArray()
    }

    @OutputDirectory
    @SuppressWarnings("GroovyUnusedDeclaration")
    File getOutputDirectory() {
        project.file(configuration.generator.target.directory)
    }

    @TaskAction
    @SuppressWarnings("GroovyUnusedDeclaration")
    void generate() {
        def configFile = new File(temporaryDir, "config.xml")
        def execResult = executeJooq(configFile)
        if (execResultHandler) {
            execResultHandler.execute(execResult)
        }
    }

    private ExecResult executeJooq(File configFile) {
        project.javaexec(new Action<JavaExecSpec>() {

            @Override
            void execute(JavaExecSpec spec) {
                spec.main = "org.jooq.codegen.GenerationTool"
                spec.classpath = jooqClasspath
                spec.args configFile
                if (javaExecSpec) {
                    javaExecSpec.execute(spec)
                }

                // Required in order to ensure correct interpretation of destination
                spec.workingDir = project.projectDir

                configFile.parentFile.mkdirs()
                configFile.bytes = getConfigurationBytes()
            }

        })
    }

}
