package nu.studer.gradle.jooq;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;

public class JooqExtension {

    private final NamedDomainObjectContainer<JooqConfig> configurations;

    @Inject
    public JooqExtension(ObjectFactory objects) {
        this.configurations = objects.domainObjectContainer(JooqConfig.class, name -> objects.newInstance(JooqConfig.class, name));
    }

    @SuppressWarnings("unused")
    public NamedDomainObjectContainer<JooqConfig> getConfigurations() {
        return configurations;
    }

}
