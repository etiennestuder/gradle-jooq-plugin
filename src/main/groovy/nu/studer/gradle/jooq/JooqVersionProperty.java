package nu.studer.gradle.jooq;

import org.gradle.api.Project;

import static java.util.Objects.requireNonNull;

final class JooqVersionProperty {

    private static final String PROJECT_PROPERTY = "jooqVersion";
    private static final String DEFAULT = "3.13.2";

    final String version;

    private JooqVersionProperty(String version) {
        this.version = requireNonNull(version);
    }

    static void applyDefaultVersion(Project project) {
        project.getExtensions().getExtraProperties().set(PROJECT_PROPERTY, DEFAULT);
    }

    static JooqVersionProperty fromProject(Project project) {
        return from((String) project.getExtensions().getExtraProperties().get(PROJECT_PROPERTY));
    }

    private static JooqVersionProperty from(String version) {
        return new JooqVersionProperty(version);
    }

    String asVersion() {
        return version;
    }

}
