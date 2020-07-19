package nu.studer.gradle.jooq.property;

import org.gradle.api.Project;

import static java.util.Objects.requireNonNull;

public final class JooqGenerateSchemaSourceOnCompilationProperty {

    private static final String PROJECT_PROPERTY = "jooqGenerateSchemaSourceOnCompilation";
    private static final Boolean DEFAULT = true;

    private final Boolean value;

    private JooqGenerateSchemaSourceOnCompilationProperty(Boolean value) {
        this.value = requireNonNull(value);
    }

    public static void applyDefaultValue(Project project) {
        project.getExtensions().getExtraProperties().set(PROJECT_PROPERTY, DEFAULT);
    }

    public static JooqGenerateSchemaSourceOnCompilationProperty fromProject(Project project) {
        return from((Boolean) project.getExtensions().getExtraProperties().get(PROJECT_PROPERTY));
    }

    private static JooqGenerateSchemaSourceOnCompilationProperty from(Boolean value) {
        return new JooqGenerateSchemaSourceOnCompilationProperty(value);
    }

    public boolean asValue() {
        return value;
    }

}
