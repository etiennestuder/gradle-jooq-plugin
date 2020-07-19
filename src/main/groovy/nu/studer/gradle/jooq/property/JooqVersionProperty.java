package nu.studer.gradle.jooq.property;

import org.gradle.api.Project;

import static java.util.Objects.requireNonNull;

public final class JooqVersionProperty {

    private static final String PROJECT_PROPERTY = "jooqVersion";
    private static final String DEFAULT = "3.13.2";

    private final String version;

    private JooqVersionProperty(String version) {
        this.version = requireNonNull(version);
    }

    public static void applyDefaultVersion(Project project) {
        project.getExtensions().getExtraProperties().set(PROJECT_PROPERTY, DEFAULT);
    }

    public static JooqVersionProperty fromProject(Project project) {
        return from((String) project.getExtensions().getExtraProperties().get(PROJECT_PROPERTY));
    }

    private static JooqVersionProperty from(String version) {
        return new JooqVersionProperty(version);
    }

    public String asVersion() {
        return version;
    }

}
