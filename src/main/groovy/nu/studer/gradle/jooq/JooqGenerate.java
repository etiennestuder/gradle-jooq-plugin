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
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.gradle.process.JavaExecSpec;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Strategy;
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

import static nu.studer.gradle.jooq.util.Objects.cloneObject;

/**
 * Gradle Task that runs the jOOQ source code generation.
 */
public class JooqGenerate extends DefaultTask {

    private final Configuration jooqConfiguration;
    private final Provider<Configuration> normalizedJooqConfiguration;
    private final ConfigurableFileCollection runtimeClasspath;
    private final DirectoryProperty outputDir;
    private final Property<Boolean> upToDateable;
    private Action<? super Configuration> generationToolNormalization;
    private Action<? super JavaExecSpec> javaExecSpec;
    private Action<? super ExecResult> execResultHandler;

    private final ProjectLayout projectLayout;
    private final ExecOperations execOperations;
    private final FileSystemOperations fileSystemOperations;

    private static final Action<Configuration> OUTPUT_DIRECTORY_NORMALIZATION = c -> c.getGenerator().getTarget().setDirectory(null);

    @Inject
    public JooqGenerate(JooqConfig config, FileCollection runtimeClasspath, ObjectFactory objects, ProviderFactory providers, ProjectLayout projectLayout, ExecOperations execOperations, FileSystemOperations fileSystemOperations) {
        this.jooqConfiguration = config.getJooqConfiguration();
        this.normalizedJooqConfiguration = normalizedJooqConfigurationProvider(objects, providers);
        this.runtimeClasspath = objects.fileCollection().from(runtimeClasspath);
        this.outputDir = objects.directoryProperty().value(config.getOutputDir());
        this.upToDateable = objects.property(Boolean.class).convention(Boolean.FALSE);

        this.projectLayout = projectLayout;
        this.execOperations = execOperations;
        this.fileSystemOperations = fileSystemOperations;

        // do not use lambda due to a bug in Gradle 6.5
        getOutputs().upToDateWhen(new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task task) {
                return upToDateable.get();
            }
        });
    }

    private Provider<Configuration> normalizedJooqConfigurationProvider(ObjectFactory objects, ProviderFactory providers) {
        Property<Configuration> normalizedConfiguration = objects.property(Configuration.class);
        normalizedConfiguration.set(providers.provider(() -> {
            Configuration clonedConfiguration = cloneObject(jooqConfiguration);
            OUTPUT_DIRECTORY_NORMALIZATION.execute(clonedConfiguration);
            if (generationToolNormalization != null) {
                generationToolNormalization.execute(clonedConfiguration);
            }
            return clonedConfiguration;
        }));
        normalizedConfiguration.finalizeValueOnRead();
        return normalizedConfiguration;
    }

    @Input
    public Provider<Configuration> getNormalizedJooqConfiguration() {
        return normalizedJooqConfiguration;
    }

    @Classpath
    public FileCollection getRuntimeClasspath() {
        return runtimeClasspath;
    }

    @OutputDirectory
    public Provider<Directory> getOutputDir() {
        return outputDir;
    }

    @Internal
    public Property<Boolean> getUpToDateable() {
        return upToDateable;
    }

    @Internal
    public Action<? super JavaExecSpec> getJavaExecSpec() {
        return javaExecSpec;
    }

    @SuppressWarnings("unused")
    public void setJavaExecSpec(Action<? super JavaExecSpec> javaExecSpec) {
        this.javaExecSpec = javaExecSpec;
    }

    @Internal
    public Action<? super ExecResult> getExecResultHandler() {
        return execResultHandler;
    }

    @SuppressWarnings("unused")
    public void setExecResultHandler(Action<? super ExecResult> execResultHandler) {
        this.execResultHandler = execResultHandler;
    }

    @Internal
    public Action<? super Configuration> getGenerationToolNormalization() {
        return generationToolNormalization;
    }

    @SuppressWarnings("unused")
    public void setGenerationToolNormalization(Action<? super Configuration> generationToolNormalization) {
        this.generationToolNormalization = generationToolNormalization;
    }

    @TaskAction
    public void generate() {
        // avoid excessive and/or schema-violating XML being created due to the serialization of default values
        trimConfiguration(jooqConfiguration);

        // set target directory to the defined default value if no explicit value has been configured
        jooqConfiguration.getGenerator().getTarget().setDirectory(outputDir.get().getAsFile().getAbsolutePath());

        // clean target directory to ensure no stale files are still around
        fileSystemOperations.delete(spec -> spec.delete(outputDir.get()));

        // define a config file to which the jOOQ code generation configuration is written to
        File configFile = new File(getTemporaryDir(), "config.xml");

        // write jOOQ code generation configuration to config file
        writeConfiguration(jooqConfiguration, configFile);

        // generate the jOOQ Java sources files using the written config file
        ExecResult execResult = executeJooq(configFile);

        // invoke custom result handler
        if (execResultHandler != null) {
            execResultHandler.execute(execResult);
        }
    }

    @SuppressWarnings("deprecation")
    private void trimConfiguration(Configuration configuration) {
        // avoid default value (name) being written even when matchers are configured
        Generator generator = configuration.getGenerator();
        if (generator != null) {
            Strategy strategy = generator.getStrategy();
            if (strategy != null && strategy.getMatchers() != null) {
                strategy.setName(null);
            }
        }

        // avoid JDBC element being written when it has an empty configuration
        Jdbc jdbc = configuration.getJdbc();
        if (jdbc != null) {
            if (jdbc.getDriver() == null
                && jdbc.getUrl() == null
                && jdbc.getSchema() == null
                && jdbc.getUser() == null
                && jdbc.getUsername() == null
                && jdbc.getPassword() == null
                && jdbc.isAutoCommit() == null
                && jdbc.getProperties().isEmpty()
            ) {
                configuration.setJdbc(null);
            }
        }
    }

    private void writeConfiguration(Configuration config, File file) {
        try (OutputStream fs = new FileOutputStream(file)) {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(GenerationTool.class.getResource("/xsd/" + xsdFileName()));

            JAXBContext ctx = JAXBContext.newInstance(Configuration.class);
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setSchema(schema);

            marshaller.marshal(config, fs);
        } catch (IOException | JAXBException | SAXException e) {
            throw new TaskExecutionException(JooqGenerate.this, e);
        }
    }

    private String xsdFileName() {
        // use reflection to avoid inlining of the String constant org.jooq.Constants.XSD_CODEGEN
        try {
            Class<?> aClass = Class.forName("org.jooq.Constants");
            return (String) aClass.getDeclaredField("XSD_CODEGEN").get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new TaskExecutionException(JooqGenerate.this, e);
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
