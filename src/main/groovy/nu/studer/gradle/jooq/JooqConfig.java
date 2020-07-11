package nu.studer.gradle.jooq;

import groovy.lang.Closure;
import nu.studer.gradle.util.JaxbConfigurationBridge;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.jooq.meta.jaxb.Configuration;

import javax.inject.Inject;

import static java.lang.String.format;

public class JooqConfig {

    final String name;

    private final Configuration jooqConfiguration;
    private final Property<Boolean> generateSchemaSourceOnCompilation;

    @Inject
    public JooqConfig(String name, ObjectFactory objects) {
        this.name = name;

        this.jooqConfiguration = new Configuration();
        this.generateSchemaSourceOnCompilation = objects.property(Boolean.class).convention(Boolean.TRUE);
    }

    @Internal
    public Configuration getJooqConfiguration() {
        return jooqConfiguration;
    }

    @Internal
    public Property<Boolean> getGenerateSchemaSourceOnCompilation() {
        return generateSchemaSourceOnCompilation;
    }

    @SuppressWarnings("unused")
    public void generationTool(Closure<?> closure) {
        // apply the given closure to the configuration bridge, i.e. its contained JAXB Configuration object
        JaxbConfigurationBridge delegate = new JaxbConfigurationBridge(jooqConfiguration, format("jooq.%s.generationTool", name));
        Closure<?> copy = (Closure<?>) closure.clone();
        copy.setResolveStrategy(Closure.DELEGATE_FIRST);
        copy.setDelegate(delegate);
        if (copy.getMaximumNumberOfParameters() == 0) {
            copy.call();
        } else {
            copy.call(delegate);
        }
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

