package nu.studer.gradle.jooq;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public class JooqExtension {

    private static final String DEFAULT_VERSION = "3.18.7";
    private static final JooqEdition DEFAULT_EDITION = JooqEdition.OSS;

    private final Property<String> version;
    private final Property<JooqEdition> edition;
    private final NamedDomainObjectContainer<JooqConfig> configurations;

    @Inject
    public JooqExtension(ObjectFactory objects) {
        this.version = objects.property(String.class).convention(DEFAULT_VERSION);
        this.edition = objects.property(JooqEdition.class).convention(DEFAULT_EDITION);
        this.configurations = objects.domainObjectContainer(JooqConfig.class, name -> objects.newInstance(JooqConfig.class, name));

        version.finalizeValueOnRead();
        edition.finalizeValueOnRead();
    }

    @SuppressWarnings("unused")
    public Property<String> getVersion() {
        return version;
    }

    @SuppressWarnings("unused")
    public Property<JooqEdition> getEdition() {
        return edition;
    }

    @SuppressWarnings("unused")
    public NamedDomainObjectContainer<JooqConfig> getConfigurations() {
        return configurations;
    }

}
