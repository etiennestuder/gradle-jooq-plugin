package nu.studer.gradle.jooq;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.util.GradleVersion;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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
        if (GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("6.1")) < 0) {
            throw new IllegalStateException("This version of the jooq plugin is not compatible with Gradle < 6.1");
        }

        // apply Java base plugin, making it possible to also use the jOOQ plugin for Android builds
        project.getPlugins().apply(JavaBasePlugin.class);

        // add jOOQ DSL extension
        JooqExtension jooqExtension = project.getExtensions().create("jooq", JooqExtension.class);

        // create configuration for the runtime classpath of the jooq code generator (shared by all jooq configuration domain objects)
        final Configuration runtimeConfiguration = createJooqGeneratorRuntimeConfiguration(project);

        // create a jooq task for each jooq configuration domain object
        jooqExtension.getConfigurations().configureEach(config -> {
            String taskName = "generate" + (config.name.equals("main") ? "" : capitalize(config.name)) + "Jooq";
            TaskProvider<JooqGenerate> jooq = project.getTasks().register(taskName, JooqGenerate.class, config, runtimeConfiguration);
            jooq.configure(task -> {
                task.setDescription(String.format("Generates the jOOQ sources from the %s jOOQ configuration.", config.name));
                task.setGroup("jOOQ");
            });

            // add the output of the jooq task as a source directory of the source set with the matching name (which adds an implicit task dependency)
            SourceSetContainer sourceSets = getSourceSets(project);
            sourceSets.configureEach(sourceSet -> {
                if (sourceSet.getName().equals(config.name)) {
                    sourceSet.getJava().srcDir(config.getGenerateSchemaSourceOnCompilation().flatMap(b -> b ? jooq.flatMap(JooqGenerate::getOutputDir) : config.getOutputDir()));
                    project.getDependencies().add(sourceSet.getImplementationConfigurationName(), "org.jooq:jooq");
                }
            });
        });

        // use the configured jOOQ version and edition on all jOOQ dependencies
        enforceJooqEditionAndVersion(project, jooqExtension);
    }

    private SourceSetContainer getSourceSets(Project project) {
        if (isAtLeastGradleVersion("7.1")) {
            // return project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
            return getSourceSetsDeprecated(project);
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
    private static Configuration createJooqGeneratorRuntimeConfiguration(Project project) {
        Configuration jooqGeneratorRuntime = project.getConfigurations().create("jooqGenerator");
        jooqGeneratorRuntime.setDescription("The classpath used to invoke the jOOQ code generator. Add your JDBC driver, generator extensions, and additional dependencies here.");
        project.getDependencies().add(jooqGeneratorRuntime.getName(), "org.jooq:jooq-codegen");
        project.getDependencies().add(jooqGeneratorRuntime.getName(), "javax.xml.bind:jaxb-api:2.3.1");
        project.getDependencies().add(jooqGeneratorRuntime.getName(), "org.glassfish.jaxb:jaxb-core:2.3.0.1");
        project.getDependencies().add(jooqGeneratorRuntime.getName(), "org.glassfish.jaxb:jaxb-runtime:2.3.3");
        project.getDependencies().add(jooqGeneratorRuntime.getName(), "javax.activation:activation:1.1.1");
        return jooqGeneratorRuntime;
    }

    /**
     * Forces the jOOQ version and edition selected by the user throughout all dependency configurations.
     */
    private static void enforceJooqEditionAndVersion(Project project, JooqExtension jooqExtension) {
        Set<String> jooqGroupIds = Arrays.stream(JooqEdition.values()).map(JooqEdition::getGroupId).collect(Collectors.toSet());
        project.getConfigurations().configureEach(configuration ->
            configuration.getResolutionStrategy().eachDependency(details -> {
                ModuleVersionSelector requested = details.getRequested();
                if (jooqGroupIds.contains(requested.getGroup()) && requested.getName().startsWith("jooq")) {
                    String group = jooqExtension.getEdition().get().getGroupId();
                    String version = jooqExtension.getVersion().get();
                    details.useTarget(group + ":" + requested.getName() + ":" + version);
                }
            })
        );
    }

}
