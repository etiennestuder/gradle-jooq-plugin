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
import org.gradle.api.tasks.SourceSet
import org.jooq.util.jaxb.Configuration

/**
 * Extension point used to configure the tasks that are made available by this plugin.
 * This extension point allows separate configuration for each source set.
 */
class JooqExtension {

    final String path
    final configs
    final configuredConfigs

    JooqExtension(String path) {
        this.path = path
        this.configs = [:]
        this.configuredConfigs = [:]
    }

    def methodMissing(String sourceSetName, args) {
        if (args.length == 1 && args[0] instanceof Closure) {
            // keep track of the source sets for which configuration has been invoked via closure
            configuredConfigs[sourceSetName] = true

            // find bridge extension for the given source set
            def config = configs[sourceSetName]

            // apply the given closure to the bridge extension, i.e. its Configuration object
            def delegate = config
            Closure copy = (Closure) args[0].clone();
            copy.resolveStrategy = Closure.DELEGATE_FIRST;
            copy.delegate = delegate;
            if (copy.maximumNumberOfParameters == 0) {
                copy.call();
            } else {
                copy.call delegate;
            }

            config.target
        } else {
            throw new MissingMethodException(sourceSetName, getClass(), args)
        }
    }

    def registerSourceSet(SourceSet sourceSet) {
        def sourceSetName = sourceSet.name
        def config = new BridgeExtension(new Configuration(), "${path}.${sourceSetName}")
        configs[sourceSetName] = config
    }

    Configuration getJooqConfiguration(String sourceSetName) {
        configs[sourceSetName].target
    }

}
