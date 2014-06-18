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

import nu.studer.gradle.util.Objects
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.jooq.util.jaxb.Generator
import org.jooq.util.jaxb.Target

/**
 * Plugin that extends the Java plugin, adds an empty 'jooq' configuration inherited by the compile configuration, and
 * registers a {@link JooqTask} for each defined source set that generates the jOOQ sources from the configured
 * database. The tasks properly participate in the Gradle uptodate checks. The tasks are wired as dependencies of the
 * compilation tasks of the Java plugin.
 */
class JooqPlugin implements Plugin<Project> {

    public void apply(Project project) {
        // apply the Java plugin since jOOQ generates Java source code
        project.plugins.apply(JavaPlugin.class);

        // add a new 'jooq' configuration that the Java compile configuration will inherit from
        Configuration jooqConfiguration = project.configurations.create(JooqConstants.JOOQ_CONFIGURATION_NAME).
                setVisible(false).
                setTransitive(false).
                setDescription("Compile classpath for generated jOOQ sources.");
        project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME).extendsFrom(jooqConfiguration);

        // register extension
        JooqExtension jooqExtension = project.extensions.create(JooqConstants.JOOQ_EXTENSION_NAME,
                JooqExtension.class, JooqConstants.JOOQ_EXTENSION_NAME);

        // validate extension only contains configurations for valid source sets
        project.afterEvaluate {
            Set existingSourceSets = project.convention.getPlugin(JavaPluginConvention.class).sourceSets.names
            Set configuredSourceSets = new HashSet(jooqExtension.configs.keySet())
            configuredSourceSets.removeAll(existingSourceSets)
            if (configuredSourceSets) {
                throw new InvalidUserDataException("Extension '$JooqConstants.JOOQ_EXTENSION_NAME' contains unknown source set(s): $configuredSourceSets")
            }
        }

        // add a jOOQ task for each source set
        project.convention.getPlugin(JavaPluginConvention.class).sourceSets.all { SourceSet sourceSet ->
            org.jooq.util.jaxb.Configuration config = jooqExtension."$sourceSet.name".target

            // add a task instance that generates the jOOQ sources
            String jooqTaskName = sourceSet.getTaskName("generate", "JooqSchemaSource");
            JooqTask jooqTask = project.tasks.create(jooqTaskName, JooqTask.class);
            jooqTask.description = String.format("Generates the jOOQ %s sources from the database schema", sourceSet.name);
            jooqTask.group = "jOOQ";
            jooqTask.sourceSetName = sourceSet.name;

            // add the directory where the jOOQ sources are generated to the jOOQ configuration and to the source set
            String outputDirectoryName = String.format("%s/generated-src/jooq/%s", project.buildDir, sourceSet.name);
            config.withGenerator(new Generator().withTarget(new Target().withDirectory(outputDirectoryName)))
            sourceSet.java.srcDir { project.file(config.generator.target.directory) };

            // add a task dependency to generate the sources before the compilation takes place
            project.tasks.getByName(sourceSet.getCompileJavaTaskName()).dependsOn jooqTaskName;

            // add inputs and outputs to participate in uptodate checks
            jooqTask.inputs.property "configState", { Objects.deepHashCode config }
            jooqTask.outputs.dir { project.file(config.generator.target.directory) }
        }
    }

}
