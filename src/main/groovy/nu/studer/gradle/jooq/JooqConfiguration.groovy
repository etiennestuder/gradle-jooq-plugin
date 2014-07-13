package nu.studer.gradle.jooq

import nu.studer.gradle.util.JaxbConfigurationBridge
import org.gradle.api.tasks.SourceSet

/**
 * Represents a jOOQ configuration which consists of the actual jOOQ source code generation configuration and
 * the source set in which to include the generated sources.
 */
class JooqConfiguration {

    final SourceSet sourceSet
    final JaxbConfigurationBridge configBridge

    JooqConfiguration(SourceSet sourceSet, JaxbConfigurationBridge configBridge) {
        this.sourceSet = sourceSet
        this.configBridge = configBridge
    }

}
