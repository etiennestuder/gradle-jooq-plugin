/*
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
package nu.studer.gradle.jooq;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.gradle.process.JavaExecSpec;
import org.jooq.Constants;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Gradle Task that runs the jOOQ source code generation.
 */
@SuppressWarnings("UnstableApiUsage")
public class JooqTask extends DefaultTask {

    private final JooqConfig config;
    private final ConfigurableFileCollection runtimeClasspath;
    private Configuration normalizedConfiguration;
    private Action<? super JavaExecSpec> javaExecSpec;
    private Action<? super ExecResult> execResultHandler;

    private final ProjectLayout projectLayout;
    private final ExecOperations execOperations;

    @Inject
    public JooqTask(JooqConfig config, FileCollection runtimeClasspath, ObjectFactory objects, ProjectLayout projectLayout, ExecOperations execOperations) {
        this.config = config;
        this.runtimeClasspath = objects.fileCollection().from(runtimeClasspath);

        this.projectLayout = projectLayout;
        this.execOperations = execOperations;
    }

    @SuppressWarnings("unused")
    @Nested
    public JooqConfig getConfig() {
        return config;
    }

    @SuppressWarnings("unused")
    @Classpath
    public ConfigurableFileCollection getRuntimeClasspath() {
        return runtimeClasspath;
    }

    // todo (etst) move to JooqConfig
    @Input
    public Configuration getNormalizedConfiguration() {
        if (normalizedConfiguration == null) {
            normalizedConfiguration = relativizeTo(config.getJooqConfiguration(), projectLayout.getProjectDirectory().getAsFile());
        }
        return normalizedConfiguration;
    }

    private static Configuration relativizeTo(Configuration configuration, File dir) {
        String directoryValue = configuration.getGenerator().getTarget().getDirectory();
        if (directoryValue != null) {
            File file = new File(directoryValue);
            if (file.isAbsolute()) {
                String relativized = dir.toURI().relativize(file.toURI()).getPath();
                if (relativized.endsWith(File.separator)) {
                    relativized = relativized.substring(0, relativized.length() - 1);
                }
                configuration.getGenerator().getTarget().setDirectory(relativized);
            }
        }
        return configuration;
    }

    @Internal
    public Action<? super JavaExecSpec> getJavaExecSpec() {
        return javaExecSpec;
    }

    public void setJavaExecSpec(Action<? super JavaExecSpec> javaExecSpec) {
        this.javaExecSpec = javaExecSpec;
    }

    @Internal
    public Action<? super ExecResult> getExecResultHandler() {
        return execResultHandler;
    }

    public void setExecResultHandler(Action<? super ExecResult> execResultHandler) {
        this.execResultHandler = execResultHandler;
    }

    @OutputDirectory
    public Directory getOutputDir() {
        return config.getOutputDir();
    }

    @TaskAction
    public void generate() {
        // define a config file to which the jOOQ code generation configuration is written to
        File configFile = new File(getTemporaryDir(), "config.xml");

        // write jOOQ code generation configuration to config file
        writeConfiguration(getNormalizedConfiguration(), configFile);

        // generate the jOOQ Java sources files using the written config file
        ExecResult execResult = executeJooq(configFile);

        // invoke custom result handler
        if (execResultHandler != null) {
            execResultHandler.execute(execResult);
        }
    }

    private void writeConfiguration(Configuration config, File file) {
        try (OutputStream fs = new FileOutputStream(file)) {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(GenerationTool.class.getResource("/xsd/" + Constants.XSD_CODEGEN));

            JAXBContext ctx = JAXBContext.newInstance(Configuration.class);
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setSchema(schema);

            marshaller.marshal(config, fs);
        } catch (IOException | JAXBException | SAXException e) {
            throw new TaskExecutionException(JooqTask.this, e);
        }
    }

    private ExecResult executeJooq(final File configFile) {
        return execOperations.javaexec(spec -> {
            spec.setMain("org.jooq.codegen.GenerationTool");
            spec.setClasspath(runtimeClasspath);
            spec.setWorkingDir(projectLayout.getProjectDirectory());
            spec.args(configFile);

            if (javaExecSpec != null) {
                javaExecSpec.execute(spec);
            }
        });
    }

}
