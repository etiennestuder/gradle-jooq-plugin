package nu.studer.gradle.jooq;

import org.gradle.api.Project;

import static java.util.Objects.requireNonNull;

final class JooqGenerateSchemaSourceOnCompilationProperty {

    private static final String PROJECT_PROPERTY = "jooqGenerateSchemaSourceOnCompilation";
    private static final Boolean DEFAULT = true;

    final Boolean value;

    private JooqGenerateSchemaSourceOnCompilationProperty(Boolean value) {
        this.value = requireNonNull(value);
    }

    static void applyDefaultValue(Project project) {
        project.getExtensions().getExtraProperties().set(PROJECT_PROPERTY, DEFAULT);
    }

    static JooqGenerateSchemaSourceOnCompilationProperty fromProject(Project project) {
        return from((Boolean) project.getExtensions().getExtraProperties().get(PROJECT_PROPERTY));
    }

    private static JooqGenerateSchemaSourceOnCompilationProperty from(Boolean value) {
        return new JooqGenerateSchemaSourceOnCompilationProperty(value);
    }

    boolean asValue() {
        return value;
    }

}
