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

import nu.studer.gradle.util.Objects
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
import org.jooq.util.GenerationTool
import org.jooq.util.jaxb.Configuration

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
    Configuration configuration

    @Internal
    Action<? super JavaExecSpec> javaExecSpec

    @Internal
    Action<? super ExecResult> execResultHandler

    @InputFiles
    @Classpath
    FileCollection jooqClasspath

    @Input
    int getConfigHash() {
        Objects.deepHashCode(configuration)
    }

    @OutputDirectory
    File getOutputDirectory() {
        project.file(configuration.generator.target.directory)
    }

    @TaskAction
    void generate() {
        def configFile = new File(project.buildDir, "tmp/jooq/config.xml")
        writeConfig(configFile)
        def execResult = executeJooq(configFile)
        if (execResultHandler) {
            execResultHandler.execute(execResult)
        }
    }

    private void writeConfig(File configFile) {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        Schema schema = sf.newSchema(GenerationTool.class.getResource("/xsd/" + Constants.XSD_CODEGEN))

        JAXBContext ctx = JAXBContext.newInstance(Configuration.class)
        Marshaller marshaller = ctx.createMarshaller()
        marshaller.setSchema(schema)

        configFile.parentFile.mkdirs()
        marshaller.marshal(configuration, configFile)
    }

    private ExecResult executeJooq(File configFile) {
        project.javaexec(new Action<JavaExecSpec>() {

            @Override
            void execute(JavaExecSpec spec) {
                spec.main = "org.jooq.util.GenerationTool"
                spec.classpath = jooqClasspath
                spec.args configFile
                if (javaExecSpec) {
                    javaExecSpec.execute(spec)
                }
            }

        })
    }

}
