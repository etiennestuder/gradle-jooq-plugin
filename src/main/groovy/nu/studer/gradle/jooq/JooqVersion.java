package nu.studer.gradle.jooq;

import org.gradle.api.Project;

import java.util.Objects;

final class JooqVersion {

    private static final String PROJECT_PROPERTY = "jooqVersion";
    private static final String DEFAULT = "3.13.2";

    private final String versionString;

    private JooqVersion(String versionString) {
        this.versionString = versionString;
    }

    static void applyDefaultVersion(Project project) {
        project.getExtensions().getExtraProperties().set(PROJECT_PROPERTY, DEFAULT);
    }

    static JooqVersion fromProject(Project project) {
        return from(Objects.requireNonNull(project.getExtensions().getExtraProperties().get(PROJECT_PROPERTY)).toString());
    }

    private static JooqVersion from(String versionString) {
        return new JooqVersion(versionString);
    }

    String asString() {
        return versionString;
    }

}
