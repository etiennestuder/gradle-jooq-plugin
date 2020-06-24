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

    Project project
    JooqExtension extension
    Configuration jooqRuntime

    void apply(Project project) {
        // abort if old Gradle version is not supported
        if (GradleVersion.current().baseVersion < GradleVersion.version("5.0")) {
            throw new IllegalStateException("This version of the jooq plugin is not compatible with Gradle < 5.0");
        }

        this.project = project

        project.plugins.apply(JavaBasePlugin.class)
        addJooqExtension(project)
        addJooqConfiguration(project)
        forceJooqVersionAndEdition(project)
    }

    /**
     * Adds the DSL extensions that allows the user to configure key aspects of
     * this plugin.
     */
    private void addJooqExtension(Project project) {
        def whenConfigurationAdded = { JooqConfiguration jooqConfiguration ->
            createJooqTask(jooqConfiguration)
            configureDefaultOutput(jooqConfiguration)
            configureSourceSet(jooqConfiguration)
        }

        extension = project.extensions.create(JOOQ_EXTENSION_NAME, JooqExtension.class, whenConfigurationAdded, JOOQ_EXTENSION_NAME)
    }

    /**
     * Adds the configuration that holds the classpath to use for invoking jOOQ.
     * Users can add their JDBC drivers or any generator extensions they might have.
     * Explicitly add JAXB dependencies since they have been removed from JDK 9 and higher.
     * Explicitly add Activation dependency since it has been removed from JDK 11 and higher.
     */
    private void addJooqConfiguration(Project project) {
        jooqRuntime = project.configurations.create("jooqRuntime")
        jooqRuntime.setDescription("The classpath used to invoke the jOOQ generator. Add your JDBC drivers or generator extensions here.")
        project.dependencies.add(jooqRuntime.name, "org.jooq:jooq-codegen")
        project.dependencies.add(jooqRuntime.name, "javax.xml.bind:jaxb-api:2.3.1")
        project.dependencies.add(jooqRuntime.name, "javax.activation:activation:1.1.1")
        project.dependencies.add(jooqRuntime.name, "com.sun.xml.bind:jaxb-core:2.3.0.1")
        project.dependencies.add(jooqRuntime.name, "com.sun.xml.bind:jaxb-impl:2.3.0.1")
    }

    /**
     * Forces the jOOQ version and edition selected by the user throughout all
     * dependency configurations.
     */
    private void forceJooqVersionAndEdition(Project project) {
        def jooqGroupIds = JooqEdition.values().collect { it.groupId }.toSet()
        project.configurations.all {
            resolutionStrategy.eachDependency { details ->
                def requested = details.requested
                if (jooqGroupIds.contains(requested.group) && requested.name.startsWith('jooq')) {
                    details.useTarget("$extension.edition.groupId:$requested.name:$extension.version")
                }
            }
        }
    }

    /**
     * Adds the task that runs the jOOQ code generator in a separate process.
     */
    private void createJooqTask(JooqConfiguration jooqConfiguration) {
        JooqTask jooqTask = project.tasks.create(jooqConfiguration.jooqTaskName, JooqTask.class, jooqRuntime, jooqConfiguration.configuration)
        jooqTask.description = "Generates the jOOQ sources from the '$jooqConfiguration.name' jOOQ configuration."
        jooqTask.group = "jOOQ"
    }

    /**
     * Configures a sensible default output directory.
     */
    private void configureDefaultOutput(JooqConfiguration jooqConfiguration) {
        String outputDirectoryName = "${project.buildDir}/generated-src/jooq/$jooqConfiguration.name"
        jooqConfiguration.configuration.withGenerator(new Generator().withTarget(new Target().withDirectory(outputDirectoryName)))
    }

    /**
     * Ensures the Java compiler will pick up the generated sources.
     */
    private void configureSourceSet(JooqConfiguration jooqConfiguration) {
        SourceSet sourceSet = jooqConfiguration.sourceSet
        sourceSet.java.srcDir { jooqConfiguration.configuration.generator.target.directory }
        if (extension.generateSchemaSourceOnCompilation) {
            project.tasks.getByName(sourceSet.compileJavaTaskName).dependsOn jooqConfiguration.jooqTaskName.toString()
        }
    }

}
