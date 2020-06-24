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
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
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

    private final ProjectLayout projectLayout;
    private final ExecOperations execOperations;

    private Action<? super JavaExecSpec> javaExecSpec;
    private Action<? super ExecResult> execResultHandler;
    private FileCollection jooqClasspath;
    private Configuration configuration;
    private Configuration normalizedConfiguration;

    @Inject
    public JooqTask(ProjectLayout projectLayout, ExecOperations execOperations) {
        this.projectLayout = projectLayout;
        this.execOperations = execOperations;
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

    @Classpath
    public FileCollection getJooqClasspath() {
        return jooqClasspath;
    }

    public void setJooqClasspath(FileCollection jooqClasspath) {
        this.jooqClasspath = jooqClasspath;
    }

    @Internal
    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Input
    public Configuration getNormalizedConfiguration() {
        if (normalizedConfiguration == null) {
            normalizedConfiguration = relativizeTo(configuration, projectLayout.getProjectDirectory().getAsFile());
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

    @OutputDirectory
    public Directory getOutputDirectory() {
        return projectLayout.getProjectDirectory().dir(configuration.getGenerator().getTarget().getDirectory());
    }

    @TaskAction
    public void generate() {
        File configFile = new File(getTemporaryDir(), "config.xml");
        ExecResult execResult = executeJooq(configFile);
        if (execResultHandler != null) {
            execResultHandler.execute(execResult);
        }
    }

    private ExecResult executeJooq(final File configFile) {
        return execOperations.javaexec(new Action<JavaExecSpec>() {

            @Override
            public void execute(JavaExecSpec spec) {
                spec.setMain("org.jooq.codegen.GenerationTool");
                spec.setClasspath(jooqClasspath);
                spec.setWorkingDir(projectLayout.getProjectDirectory());
                spec.args(configFile);

                if (javaExecSpec != null) {
                    javaExecSpec.execute(spec);
                }

                configFile.getParentFile().mkdirs();
                writeConfiguration(getNormalizedConfiguration(), configFile);
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

        });
    }

}
