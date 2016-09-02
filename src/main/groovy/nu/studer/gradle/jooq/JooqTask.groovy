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
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jooq.util.GenerationTool
import org.jooq.util.jaxb.Configuration

/**
 * Gradle Task that runs the jOOQ source code generation.
 */
class JooqTask extends DefaultTask {

    def Configuration configuration

    @Input
    def getConfigHash() {
        Objects.deepHashCode(configuration)
    }

    @OutputDirectory
    def getOutputDirectory () {
        configuration.generator.target.directory
    }

    @TaskAction
    public void generate() {
        new GenerationTool().run(configuration)
    }
}
