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

import org.gradle.api.InvalidUserDataException
import org.jooq.Constants
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.xml.bind.annotation.XmlElement
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType

import static nu.studer.gradle.jooq.util.Objects.applyClosureToDelegate

/**
 * Generically maps from a Gradle configuration Closure to a (nested) JAXB configuration target object.
 */
class JaxbConfigurationBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaxbConfigurationBridge.class);

    private static final GString XSD_REFERENCE = "Please check the current XSD: https://www.jooq.org/xsd/${Constants.XSD_CODEGEN}."

    final Object target
    final String path

    JaxbConfigurationBridge(Object target, String path) {
        this.target = target
        this.path = path
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def methodMissing(String methodName, args) {
        if (args.length == 1 && args[0] instanceof Closure) {
            // invoke the bean getter method
            def targetMethod = target.class.methods.find { it.name == "get${methodName.capitalize()}" }
            if (targetMethod == null) {
                throw new InvalidUserDataException("Invalid configuration container element: '$methodName' on extension '$path'. " + XSD_REFERENCE)
            }

            def methodInvocationResult = targetMethod.invoke(target)

            // apply special handling if the defined return type is of type List
            if (targetMethod.returnType == List.class) {
                // if the return value is null, create a new List instance of the defined return type and set via bean setter method
                if (!methodInvocationResult) {
                    methodInvocationResult = new ArrayList()
                    target."$methodName" = methodInvocationResult
                }

                // determine the name of a list element and the element type
                Field field = target.class.declaredFields.find { it.name == "$methodName" }
                String nameOfChildren = field.getAnnotation(XmlElement).name()
                ParameterizedType elementType = field.getGenericType()
                Class classOfChildren = elementType.actualTypeArguments.first()

                // apply the given closure to the target
                def delegate = new JaxbConfigurationListBridge(methodInvocationResult, nameOfChildren, classOfChildren, "${path}.${methodName}")
                applyClosureToDelegate(args[0], delegate)
            } else {
                // if the return value is null, create a new instance of the defined return type and set via bean setter method
                if (!methodInvocationResult) {
                    methodInvocationResult = targetMethod.returnType.newInstance()
                    target."$methodName" = methodInvocationResult
                }

                // apply the given closure to the target
                def delegate = new JaxbConfigurationBridge(methodInvocationResult, "${path}.${methodName}")
                applyClosureToDelegate(args[0], delegate)
            }
        } else {
            LOGGER.warn("Cannot find configuration container element '$methodName' on '$path'. " +
                "Make sure to use the equal sign to set simple configuration property values.")
            throw new MissingMethodException(methodName, getClass(), args)
        }
    }

    def propertyMissing(String name, value) {
        if (target.hasProperty(name)) {
            target."$name" = value
        } else {
            throw new InvalidUserDataException("Invalid property: '$name' on extension '$path', value: $value. " + XSD_REFERENCE)
        }
    }

    def propertyMissing(String name) {
        // todo (etst) resolve properly
        target."$name"
    }

}
