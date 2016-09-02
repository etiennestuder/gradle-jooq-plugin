/**
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
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.jooq.util.jaxb.Generator
import org.jooq.util.jaxb.Target
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Plugin that extends the Java plugin, adds an empty 'jooq' configuration inherited by the compile configuration, and
 * registers a {@link JooqTask} for each defined jOOQ configuration. Each task generates the jOOQ source code from the
 * configured database. The tasks properly participate in the Gradle uptodate checks. The tasks are wired as dependencies
 * of the compilation tasks of the Java plugin.
 */
class JooqPlugin implements Plugin<Project> {

    static final String JOOQ_CONFIGURATION_NAME = "jooq"
    static final String JOOQ_EXTENSION_NAME = "jooq"

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqPlugin.class);

    Project project

    public void apply(Project project) {
        this.project = project

        // apply the Java plugin since jOOQ generates Java source code
        project.plugins.apply(JavaPlugin.class)
        LOGGER.debug("Applied plugin 'JavaPlugin'")

        // add a new 'jooq' configuration that the Java compile configuration will inherit from
        Configuration configuration = project.configurations.create(JOOQ_CONFIGURATION_NAME).
                setVisible(false).
                setTransitive(false).
                setDescription("Compile classpath for generated jOOQ sources.")
        project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME).extendsFrom(configuration)
        LOGGER.debug("Registered configuration '$JOOQ_CONFIGURATION_NAME'")

        def whenConfigurationAdded = { JooqConfiguration jooqConfiguration ->
            createJooqTask(jooqConfiguration)
            configureDefaultOutput(jooqConfiguration)
            configureSourceSet(jooqConfiguration)
        }

        project.extensions.create(JOOQ_EXTENSION_NAME, JooqExtension.class, whenConfigurationAdded, JOOQ_EXTENSION_NAME)
        LOGGER.debug("Registered extension '$JOOQ_EXTENSION_NAME'")
    }

    private void createJooqTask(JooqConfiguration jooqConfiguration) {
        JooqTask jooqTask = project.tasks.create(jooqConfiguration.jooqTaskName, JooqTask.class)
        jooqTask.description = "Generates the jOOQ sources from the '$jooqConfiguration.name' jOOQ configuration."
        jooqTask.group = "jOOQ"
        jooqTask.configuration = jooqConfiguration.configuration
    }

    private void configureDefaultOutput(JooqConfiguration jooqConfiguration) {
        String outputDirectoryName = "${project.buildDir}/generated-src/jooq/$jooqConfiguration.name"
        jooqConfiguration.configuration.withGenerator(new Generator().withTarget(new Target().withDirectory(outputDirectoryName)))
    }

    private void configureSourceSet(JooqConfiguration jooqConfiguration) {
        SourceSet sourceSet = jooqConfiguration.sourceSet
        sourceSet.java.srcDir { jooqConfiguration.configuration.generator.target.directory }
        project.tasks.getByName(sourceSet.compileJavaTaskName).dependsOn jooqConfiguration.jooqTaskName
    }

}
