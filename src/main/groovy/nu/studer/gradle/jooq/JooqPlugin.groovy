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

import nu.studer.gradle.util.BridgeExtension
import nu.studer.gradle.util.Objects
import org.apache.commons.lang.StringUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskOutputs
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

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqPlugin.class);

    @SuppressWarnings("GroovyAssignabilityCheck")
    public void apply(Project project) {
        // apply the Java plugin since jOOQ generates Java source code
        project.plugins.apply(JavaPlugin.class)
        LOGGER.debug("Applied plugin 'JavaPlugin'")

        // add a new 'jooq' configuration that the Java compile configuration will inherit from
        Configuration configuration = project.configurations.create(JooqConstants.JOOQ_CONFIGURATION_NAME).
                setVisible(false).
                setTransitive(false).
                setDescription("Compile classpath for generated jOOQ sources.")
        project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME).extendsFrom(configuration)
        LOGGER.debug("Registered configuration '$JooqConstants.JOOQ_CONFIGURATION_NAME'")

        // define the logic of what to do when a new jOOQ configuration is declared in the 'jooq' extension
        Closure<TaskOutputs> configHandler = { String jooqConfigName, JooqConfiguration jooqConfiguration ->
            // add a task instance that generates the jOOQ sources for the given jOOQ configuration
            String jooqTaskName = "generate${StringUtils.capitalize(jooqConfigName)}JooqSchemaSource"
            JooqTask jooqTask = project.tasks.create(jooqTaskName, JooqTask.class)
            jooqTask.description = "Generates the jOOQ sources from the '$jooqConfigName' jOOQ configuration."
            jooqTask.group = "jOOQ"
            jooqTask.jooqConfiguration = jooqConfiguration
            LOGGER.debug("Registered task '$jooqTask.name'")

            // get the jOOQ-specific configuration object and the target source set from the given jOOQ configuration
            BridgeExtension configBridge = jooqConfiguration.configBridge
            org.jooq.util.jaxb.Configuration config = configBridge.target
            SourceSet sourceSet = jooqConfiguration.sourceSet

            // add the default directory where the jOOQ sources are generated to the jOOQ configuration and to the source set
            String outputDirectoryName = "${project.buildDir}/generated-src/jooq/${sourceSet.name}/$jooqConfigName"
            config.withGenerator(new Generator().withTarget(new Target().withDirectory(outputDirectoryName)))
            sourceSet.java.srcDir { project.file(config.generator.target.directory) }

            // add a task dependency to generate the sources before the compilation takes place
            project.tasks.getByName(sourceSet.compileJavaTaskName).dependsOn jooqTaskName

            // add inputs and outputs to participate in uptodate checks
            jooqTask.inputs.property "configBridge", { Objects.deepHashCode configBridge }
            jooqTask.inputs.property "configSourceSet", { Objects.deepHashCode sourceSet.name }
            jooqTask.outputs.dir { project.file(config.generator.target.directory) }
        }

        // register the 'jooq' extension
        project.extensions.create(JooqConstants.JOOQ_EXTENSION_NAME,
                JooqExtension.class, project, configHandler, JooqConstants.JOOQ_EXTENSION_NAME)
        LOGGER.debug("Registered extension '$JooqConstants.JOOQ_EXTENSION_NAME'")
    }

}
