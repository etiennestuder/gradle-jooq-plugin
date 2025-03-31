package nu.studer.gradle.jooq;

import groovy.lang.Closure;
import nu.studer.gradle.jooq.jaxb.JaxbConfigurationBridge;
import org.gradle.api.Action;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Strategy;
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

        this.jooqConfiguration = jooqDefaultConfiguration();
        this.generateSchemaSourceOnCompilation = objects.property(Boolean.class).convention(true);
        this.outputDir = layout.getProjectDirectory()
            .dir(providers.<CharSequence>provider(() -> jooqConfiguration.getGenerator().getTarget().getDirectory()))
            .orElse(layout.getBuildDirectory().dir("generated-src/jooq/" + name));
    }

    private Configuration jooqDefaultConfiguration() {
        return new Configuration()
            .withJdbc(new Jdbc())
            .withGenerator(new Generator()
                .withStrategy(new Strategy())
                .withDatabase(new Database())
                .withGenerate(new Generate())
                .withTarget(new Target()
                    .withDirectory(null)
                )
            );
    }
    
    public String getName() {
        return name;
    }
    
    public Configuration getJooqConfiguration() {
        return jooqConfiguration;
    }

    public void jooqConfiguration(Action<? super Configuration> action) {
        action.execute(jooqConfiguration);
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

}

