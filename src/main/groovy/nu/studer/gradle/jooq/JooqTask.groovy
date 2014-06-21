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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import org.jooq.util.GenerationTool
import org.jooq.util.jaxb.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Gradle Task that runs the jOOQ code generation.
 */
class JooqTask extends DefaultTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqTask.class);

    def sourceSetName

    @SuppressWarnings("GroovyUnusedDeclaration")
    @TaskAction
    public void generate() {
        LOGGER.info("Performing jOOQ source code generation.");

        // get the jOOQ configuration for the given source set and only proceed if the
        // configuration has been configured explicitly in the build script
        // this check is necessary since a jOOQ configuration is always created for each source set by the plugin
        JooqExtension jooqExtension = (JooqExtension) project.property(JooqConstants.JOOQ_EXTENSION_NAME);
        if (jooqExtension.configuredConfigs["$sourceSetName"]) {
            Configuration config = jooqExtension.getJooqConfiguration("$sourceSetName")
            config.generator.target.directory = project.file(config.generator.target.directory).absolutePath
            new GenerationTool().run config;
        } else {
            throw new StopExecutionException("Source set '$sourceSetName' has not been configured explicitly")
        }
    }

}
