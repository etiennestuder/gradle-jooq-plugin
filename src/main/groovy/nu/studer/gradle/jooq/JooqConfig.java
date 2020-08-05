package nu.studer.gradle.jooq;

import groovy.lang.Closure;
import nu.studer.gradle.jooq.jaxb.JaxbConfigurationBridge;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
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
    private final Provider<Directory> outputDir;

    @Inject
    public JooqConfig(String name, ObjectFactory objects, ProviderFactory providers, ProjectLayout layout) {
        this.name = name;

        this.jooqConfiguration = new Configuration().withGenerator(new Generator().withTarget(new Target().withDirectory(null)));
        this.generateSchemaSourceOnCompilation = objects.property(Boolean.class).convention(true);
        this.outputDir = layout.getProjectDirectory()
            .dir(providers.<CharSequence>provider(() -> jooqConfiguration.getGenerator() != null ? jooqConfiguration.getGenerator().getTarget() != null ? jooqConfiguration.getGenerator().getTarget().getDirectory() : null : null))
            .orElse(layout.getBuildDirectory().dir("generated-src/jooq/" + name));
    }

    public Configuration getJooqConfiguration() {
        return jooqConfiguration;
    }

    public Property<Boolean> getGenerateSchemaSourceOnCompilation() {
        return generateSchemaSourceOnCompilation;
    }

    public Provider<Directory> getOutputDir() {
        return outputDir;
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
            ", outputDir=" + outputDir +
            '}';
    }

}

