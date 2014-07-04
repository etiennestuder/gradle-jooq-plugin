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
import org.gradle.api.tasks.TaskAction
import org.jooq.util.GenerationTool
import org.jooq.util.jaxb.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * Gradle Task that runs the jOOQ source code generation.
 */
class JooqTask extends DefaultTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqTask.class);

    def JooqConfiguration jooqConfiguration

    @SuppressWarnings(["GroovyUnusedDeclaration", "GroovyAssignabilityCheck"])
    @TaskAction
    public void generate() {
        // generate the jOOQ schema sources for the given configuration
        Configuration config = jooqConfiguration.configBridge.target
        config.generator.target.directory = project.file(config.generator.target.directory).absolutePath
        new GenerationTool().run config;
        LOGGER.debug("Performed jOOQ source code generation.");

    }

}
