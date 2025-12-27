package nu.studer.gradle.jooq;

import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;
import org.gradle.process.JavaExecSpec;

/**
 * Isolates Gradle toolchain-related types, introduced in 6.7 and above.
 */
abstract class ToolchainHelper {

    static void configureJavaLauncher(Property<Object> launcher, ExtensionContainer extensions) {
        JavaToolchainSpec toolchain = extensions.getByType(JavaPluginExtension.class).getToolchain();
        JavaToolchainService service = extensions.getByType(JavaToolchainService.class);
        Provider<JavaLauncher> defaultLauncher = service.launcherFor(toolchain);
        launcher.convention(defaultLauncher);
    }

    static void applyJavaLauncher(Property<Object> launcher, JavaExecSpec spec) {
        if (launcher.isPresent() && launcher.get() instanceof JavaLauncher) {
            spec.setExecutable(((JavaLauncher) launcher.get()).getExecutablePath().getAsFile().getAbsolutePath());
        }
    }

    private ToolchainHelper() {
    }

}
