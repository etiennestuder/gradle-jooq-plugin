package nu.struder.gradle.jooq

import nu.studer.gradle.jooq.JooqConfiguration
import nu.studer.gradle.jooq.JooqExtension
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jooq.meta.jaxb.*
import org.jooq.meta.jaxb.Target

/**
 * Applies the supplied action to the Project's instance of [JooqExtensionKotlin]
 *
 * ```
 * jooq {
 *  val java = project.the<JavaPluginConvention>()
 *  "schema"(java.sourceSets.getByName("main")) {
 *      jdbc {
 *          ....
 *      }
 *      generator {
 *          database {
 *              ...
 *          }
 *          target {
 *              ...
*           }
 *      }
 * }
 * ```
 * @receiver [Project] The project for which the plugin configuration will be applied
 * @param action A configuration lambda to apply on a receiver of type [JooqExtensionKotlin]
 */
fun Project.jooq(action: JooqExtensionKotlin.() -> Unit) {
    project.configure<JooqExtension> {
        JooqExtensionKotlin(this).apply(action)
    }
}

/**
 * Applies jdbc configuration to [Configuration]
 *
 * @receiver the Jooq [Configuration]
 * @param action A configuration lambda to apply on a receiver of type [Configuration]
 */
fun Configuration.jdbc(action: Jdbc.() -> Unit) {
    this.withJdbc(Jdbc().apply(action))
}

/**
 * Applies generator configuration to [Configuration]
 *
 * @receiver the Jooq [Configuration]
 * @param action A configuration lambda to apply on a receiver of type [Configuration]
 */
fun Configuration.generator(action: Generator.() -> Unit) {
    this.withGenerator(Generator().apply(action))
}

/**
 * Applies database configuration to [Generator]
 *
 * @receiver the Jooq [Generator]
 * @param action A configuration lambda to apply on a receiver of type [Generator]
 */
fun Generator.database(action: Database.() -> Unit) {
    this.withDatabase(Database().apply(action))
}

/**
 * Applies target configuration to [Generator]
 *
 * @receiver the Jooq [Generator]
 * @param action A configuration lambda to apply on a receiver of type [Generator]
 */
fun Generator.target(action: Target.() -> Unit) {
    this.withTarget(Target().apply(action))
}

/**
 * JooqExtension Wrapper that allows us to dynamically create configurations
 */
class JooqExtensionKotlin(private val jooq: JooqExtension) {

    operator fun String.invoke(sourceSet: SourceSet, action: Configuration.() -> Unit) {
        val jooqConfig = JooqConfiguration(
            this,
            sourceSet,
            Configuration()
        )
        jooq.whenConfigAdded.invoke(jooqConfig)
        jooqConfig.configuration.apply(action)
    }
}