package nu.studer.gradle.jooq;

import groovy.lang.Closure;
import nu.studer.gradle.jooq.jaxb.JaxbConfigurationBridge;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Target;

import javax.inject.Inject;

import static java.lang.String.format;
import static nu.studer.gradle.jooq.util.Objects.applyClosureToDelegate;

public class JooqConfig {

    final String name;

    private final Configuration jooqConfiguration;
    private final Property<Boolean> generateSchemaSourceOnCompilation;

    private final ProjectLayout layout;

    @Inject
    public JooqConfig(String name, ObjectFactory objects, ProjectLayout layout) {
        this.name = name;

        this.jooqConfiguration = new Configuration();
        this.generateSchemaSourceOnCompilation = objects.property(Boolean.class).convention(Boolean.TRUE);

        this.layout = layout;

        // todo (etst) add as property to jooq config
        Directory outputDirectoryName = layout.getBuildDirectory().dir("generated-src/jooq/" + name).get();
        jooqConfiguration.withGenerator(new Generator().withTarget(new Target().withDirectory(outputDirectoryName.getAsFile().getAbsolutePath())));
    }

    @Internal
    public Configuration getJooqConfiguration() {
        return jooqConfiguration;
    }

    @Internal
    public Property<Boolean> getGenerateSchemaSourceOnCompilation() {
        return generateSchemaSourceOnCompilation;
    }

    @Internal
    public Directory getOutputDir() {
        return layout.getProjectDirectory().dir(jooqConfiguration.getGenerator().getTarget().getDirectory());
    }

    @SuppressWarnings("unused")
    public void generationTool(Closure<?> closure) {
        // apply the given closure to the configuration bridge, i.e. its contained JAXB Configuration object
        JaxbConfigurationBridge delegate = new JaxbConfigurationBridge(jooqConfiguration, format("jooq.%s.generationTool", name));
        applyClosureToDelegate(closure, delegate);
    }

    @Override
    public String toString() {
        return "JooqConfig{" +
            "name='" + name + '\'' +
            ", jooqConfiguration=" + jooqConfiguration +
            ", generateSchemaSourceOnCompilation=" + generateSchemaSourceOnCompilation +
            '}';
    }

}

