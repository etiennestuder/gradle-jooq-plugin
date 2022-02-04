package nu.studer.gradle.jooq;

import nu.studer.gradle.jooq.util.Gradles;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;
import org.gradle.process.JavaExecSpec;

/**
 * Isolates Gradle toolchain related types, introduced in 6.7 and above.
 */
abstract class ToolchainHelper {

    private static final String INITIAL_TOOLCHAIN_SUPPORT = "6.7";

    static void tryConfigureJavaLauncher(Property<Object> launcher, ExtensionContainer extensions) {
        if (Gradles.isAtLeastGradleVersion(INITIAL_TOOLCHAIN_SUPPORT)) {
            JavaToolchainSpec toolchain = extensions.getByType(JavaPluginExtension.class).getToolchain();
            JavaToolchainService service = extensions.getByType(JavaToolchainService.class);
            Provider<JavaLauncher> defaultLauncher = service.launcherFor(toolchain);
            launcher.convention(defaultLauncher);
        }
    }

    static void tryApplyJavaLauncher(Property<Object> launcher, JavaExecSpec spec) {
        if (Gradles.isAtLeastGradleVersion(INITIAL_TOOLCHAIN_SUPPORT) && launcher.isPresent() && launcher.get() instanceof JavaLauncher) {
            spec.setExecutable(((JavaLauncher) launcher.get()).getExecutablePath().getAsFile().getAbsolutePath());
        } else if (!Gradles.isAtLeastGradleVersion(INITIAL_TOOLCHAIN_SUPPORT) && launcher.isPresent()) {
            throw new IllegalArgumentException("Toolchain support requires Gradle " + INITIAL_TOOLCHAIN_SUPPORT);
        }
    }

    private ToolchainHelper() {
    }

}
