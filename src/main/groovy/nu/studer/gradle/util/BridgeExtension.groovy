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
package nu.studer.gradle.util

import org.gradle.api.InvalidUserDataException

/**
 * Generic Gradle Extension element to map (nested) configuration properties to a given target object.
 */
class BridgeExtension {

    final Object target
    final String path

    BridgeExtension(Object target, String path) {
        this.target = target
        this.path = path
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def methodMissing(String methodName, args) {
        if (args.length == 1 && args[0] instanceof Closure) {
            // invoke the bean getter method
            def targetMethod = target.class.methods.find { it.name == "get${methodName.capitalize()}" }
            def methodInvocationResult = targetMethod.invoke(target)

            // if the return value is null, create a new instance of the defined return type and set via bean setter method
            if (!methodInvocationResult) {
                methodInvocationResult = targetMethod.returnType.newInstance()
                target."$methodName" = methodInvocationResult
            }

            // apply the given closure to the target
            def delegate = new BridgeExtension(methodInvocationResult, "${path}.${methodName}")
            Closure copy = (Closure) args[0].clone();
            copy.resolveStrategy = Closure.DELEGATE_FIRST;
            copy.delegate = delegate
            if (copy.maximumNumberOfParameters == 0) {
                copy.call();
            } else {
                copy.call delegate;
            }

            target
        } else {
            throw new MissingMethodException(methodName, getClass(), args)
        }
    }

    def propertyMissing(String name, value) {
        if (target.hasProperty(name)) {
            target."$name" = value
        } else {
            throw new InvalidUserDataException("Invalid property: '$name' on extension '$path', value: $value")
        }
    }

    def propertyMissing(String name) {
        target."$name"
    }

}
