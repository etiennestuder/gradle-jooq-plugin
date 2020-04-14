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

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.tasks.SourceSet
import org.jooq.meta.jaxb.Configuration

/**
 * Represents a jOOQ configuration which consists of the actual jOOQ source code generation configuration and
 * the source set in which to include the generated sources.
 */
class JooqConfiguration implements Named {

    final String name
    final SourceSet sourceSet
    final Configuration configuration
    final Action<Configuration> customNormalization

    JooqConfiguration(String name, SourceSet sourceSet, Configuration configuration, Action<Configuration> customNormalization) {
        this.name = name
        this.sourceSet = sourceSet
        this.configuration = configuration
        this.customNormalization = customNormalization
    }

    def getJooqTaskName() {
        "generate${name.capitalize()}JooqSchemaSource"
    }

}
