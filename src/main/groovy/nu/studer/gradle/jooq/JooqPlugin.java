package nu.studer.gradle.jooq;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.util.GradleVersion;

import static nu.studer.gradle.jooq.util.Gradles.isAtLeastGradleVersion;
import static nu.studer.gradle.jooq.util.Strings.capitalize;

/**
 * Plugin that extends the java-base plugin and registers a {@link JooqGenerate} task for each defined jOOQ configuration. Each task generates the jOOQ source code from the
 * configured database. The tasks properly participate in the Gradle up-to-date checks. The tasks are wired as dependencies of the compilation tasks of the JavaBasePlugin plugin.
 */
@SuppressWarnings("unused")
public class JooqPlugin implements Plugin<Project> {

    public void apply(Project project) {
        // abort if old Gradle version is not supported
        if (GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("8.6")) < 0) {
            throw new IllegalStateException("This version of the jooq plugin is not compatible with Gradle < 8.6");
        }

        // apply Java base plugin, making it possible to also use the jOOQ plugin for Android builds
        project.getPlugins().apply(JavaBasePlugin.class);

        // add jOOQ DSL extension
        JooqExtension jooqExtension = project.getExtensions().create("jooq", JooqExtension.class);

        // create configuration for the runtime classpath of the jooq code generator (shared by all jooq configuration domain objects)
        Configuration jooqGeneratorRuntimeConfiguration = createJooqGeneratorRuntimeConfiguration(project, jooqExtension);

        // create a jooq task for each jooq configuration domain object
        jooqExtension.getConfigurations().configureEach(config -> {
            String taskName = "generate" + (config.name.equals("main") ? "" : capitalize(config.name)) + "Jooq";
            TaskProvider<JooqGenerate> jooq = project.getTasks().register(taskName, JooqGenerate.class, config, jooqGeneratorRuntimeConfiguration, project.getExtensions());
            jooq.configure(task -> {
                task.setDescription(String.format("Generates the jOOQ sources from the %s jOOQ configuration.", config.name));
                task.setGroup("jOOQ");
            });

            // add the output of the jooq task as a source directory of the source set with the matching name (which adds an implicit task dependency)
            SourceSetContainer sourceSets = getSourceSets(project);
            sourceSets.configureEach(sourceSet -> {
                if (sourceSet.getName().equals(config.name)) {
                    sourceSet.getJava().srcDir(config.getGenerateSchemaSourceOnCompilation().flatMap(b -> b ? jooq.flatMap(JooqGenerate::getOutputDir) : config.getOutputDir()));
                    project.getDependencies().addProvider(sourceSet.getImplementationConfigurationName(),
                            jooqExtension.getEdition().map(e -> e.getGroupId() + ":jooq").flatMap(ga -> jooqExtension.getVersion().map(v -> ga + ":" + v)));
                }
            });
        });
    }

    private SourceSetContainer getSourceSets(Project project) {
        if (isAtLeastGradleVersion("8.0")) {
            return project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
        } else {
            return getSourceSetsDeprecated(project);
        }
    }

    @SuppressWarnings("deprecation")
    private SourceSetContainer getSourceSetsDeprecated(Project project) {
        return project.getConvention().getPlugin(org.gradle.api.plugins.JavaPluginConvention.class).getSourceSets();
    }

    /**
     * Adds the configuration that holds the classpath to use for invoking jOOQ. Users can add their JDBC driver and any generator extensions they might have. Explicitly add JAXB
     * dependencies since they have been removed from JDK 9 and higher. Explicitly add Activation dependency since it has been removed from JDK 11 and higher.
     */
    private static Configuration createJooqGeneratorRuntimeConfiguration(Project project, JooqExtension jooqExtension) {
        Configuration jooqGeneratorRuntime = project.getConfigurations().create("jooqGenerator");
        jooqGeneratorRuntime.setDescription("The classpath used to invoke the jOOQ code generator. Add your JDBC driver, generator extensions, and additional dependencies here.");
        project.getDependencies().addProvider(jooqGeneratorRuntime.getName(),
                jooqExtension.getEdition().map(e -> e.getGroupId() + ":jooq-codegen").flatMap(ga -> jooqExtension.getVersion().map(v -> ga + ":" + v)));
        return jooqGeneratorRuntime;
    }

}
