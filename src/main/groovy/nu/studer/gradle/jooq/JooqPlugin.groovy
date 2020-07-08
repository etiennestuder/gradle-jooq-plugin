/*
 Copyright 2014 Etienne Studer

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package nu.studer.gradle.jooq

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.util.GradleVersion
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Target

/**
 * Plugin that extends the java-base plugin and registers a {@link JooqTask} for each defined jOOQ configuration.
 * Each task generates the jOOQ source code from the configured database. The tasks properly participate in the Gradle
 * up-to-date checks. The tasks are wired as dependencies of the compilation tasks of the JavaBasePlugin plugin.
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class JooqPlugin implements Plugin<Project> {

    private static final String JOOQ_EXTENSION_NAME = "jooq"

    void apply(Project project) {
        // abort if old Gradle version is not supported
        if (GradleVersion.current().baseVersion < GradleVersion.version("6.0")) {
            throw new IllegalStateException("This version of the jooq plugin is not compatible with Gradle < 6.0")
        }

        // apply Java base plugin, making it possible to also use the jOOQ plugin for Android builds
        project.plugins.apply(JavaBasePlugin.class)

        // allow to configure the jOOQ edition/version and compilation on source code generation via extension property
        JooqEditionProperty.applyDefaultEdition(project)
        JooqVersionProperty.applyDefaultVersion(project)
        JooqGenerateSchemaSourceOnCompilationProperty.applyDefaultValue(project)

        // use the configured jOOQ version on all jOOQ dependencies
        enforceJooqEditionAndVersion(project)

        // create configuration for the runtime classpath of the jooq code generator (shared by all jooq configuration domain objects)
        final Configuration runtimeConfiguration = createJooqRuntimeConfiguration(project);

        project.extensions.create(JOOQ_EXTENSION_NAME, JooqExtension.class, { JooqConfiguration jooqConfiguration, JooqExtension extension ->
            JooqTask jooqTask = project.tasks.create(jooqConfiguration.jooqTaskName, JooqTask.class, runtimeConfiguration, jooqConfiguration.configuration)
            jooqTask.description = "Generates the jOOQ sources from the '${jooqConfiguration.name}' jOOQ configuration."
            jooqTask.group = "jOOQ"

            String outputDirectoryName = project.layout.buildDirectory.dir("generated-src/jooq/${jooqConfiguration.name}").get()
            jooqConfiguration.configuration.withGenerator(new Generator().withTarget(new Target().withDirectory(outputDirectoryName)))

            SourceSet sourceSet = jooqConfiguration.sourceSet
            boolean generateSchemaSourceOnCompilationProperty = JooqGenerateSchemaSourceOnCompilationProperty.fromProject(project).asValue()
            if (generateSchemaSourceOnCompilationProperty) {
                sourceSet.java.srcDir jooqTask
            } else {
                sourceSet.java.srcDir { jooqTask.outputDirectory }
            }
        }, JOOQ_EXTENSION_NAME)
    }

    /**
     * Forces the jOOQ version and edition selected by the user throughout all
     * dependency configurations.
     */
    private static void enforceJooqEditionAndVersion(Project project) {
        def jooqGroupIds = JooqEdition.values().collect { it.groupId }.toSet()
        project.configurations.configureEach { configuration ->
            configuration.resolutionStrategy.eachDependency { details ->
                def requested = details.requested
                if (jooqGroupIds.contains(requested.group) && requested.name.startsWith('jooq')) {
                    def group = JooqEditionProperty.fromProject(project).asGroupId()
                    def version = JooqVersionProperty.fromProject(project).asVersion()
                    details.useTarget("$group:$requested.name:$version")
                }
            }
        }

    }

    /**
     * Adds the configuration that holds the classpath to use for invoking jOOQ.
     * Users can add their JDBC driver and any generator extensions they might have.
     * Explicitly add JAXB dependencies since they have been removed from JDK 9 and higher.
     * Explicitly add Activation dependency since it has been removed from JDK 11 and higher.
     */
    private static Configuration createJooqRuntimeConfiguration(Project project) {
        Configuration jooqRuntime = project.configurations.create("jooqRuntime")
        jooqRuntime.setDescription("The classpath used to invoke jOOQ. Add your JDBC driver and generator extensions here.")
        project.dependencies.add(jooqRuntime.name, "org.jooq:jooq-codegen")
        project.dependencies.add(jooqRuntime.name, "javax.xml.bind:jaxb-api:2.3.1")
        project.dependencies.add(jooqRuntime.name, "com.sun.xml.bind:jaxb-core:2.3.0.1")
        project.dependencies.add(jooqRuntime.name, "com.sun.xml.bind:jaxb-impl:2.3.0.1")
        project.dependencies.add(jooqRuntime.name, "javax.activation:activation:1.1.1")
        jooqRuntime
    }

}
