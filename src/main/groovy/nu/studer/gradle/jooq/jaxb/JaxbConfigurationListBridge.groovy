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
package nu.studer.gradle.jooq.jaxb

import nu.studer.gradle.jooq.util.Objects
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Generically maps from a Gradle configuration Closure to a (nested) JAXB configuration target list.
 */
class JaxbConfigurationListBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaxbConfigurationListBridge.class);

    final List target
    final String nameOfChildren
    final Class classOfChildren
    final String path

    JaxbConfigurationListBridge(List target, String nameOfChildren, Class classOfChildren, String path) {
        this.target = target
        this.nameOfChildren = nameOfChildren
        this.classOfChildren = classOfChildren
        this.path = path
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def methodMissing(String methodName, args) {
        if (methodName == nameOfChildren && args.length == 1 && args[0] instanceof Closure) {
            def child = classOfChildren.newInstance()
            target.add(child)

            // apply the given closure to the target
            def delegate = new JaxbConfigurationBridge(child, "${path}.${methodName}")
            Objects.applyClosureToDelegate(args[0], delegate)

            target
        } else {
            LOGGER.error("Cannot find configuration list element '$methodName' on '$path'. " +
                    "The expected configuration list element name is '$nameOfChildren'.")
            throw new MissingMethodException(methodName, getClass(), args)
        }
    }

}
