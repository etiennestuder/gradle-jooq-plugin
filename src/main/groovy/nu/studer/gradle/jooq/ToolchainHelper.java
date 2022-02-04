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
class ToolchainHelper {

    private static final String GRADLE_VERSION_WITH_TOOLCHAIN = "7.3";

    private final boolean supportsToolchainAndConfigurationCache;
    private final Property<Object> launcher;

    ToolchainHelper(ExtensionContainer extensions, Property<Object> launcher) {
        this.launcher = launcher;
        this.supportsToolchainAndConfigurationCache = Gradles.isAtLeastGradleVersion(GRADLE_VERSION_WITH_TOOLCHAIN);
        if (supportsToolchainAndConfigurationCache) {
            JavaToolchainSpec toolchain = extensions.getByType(JavaPluginExtension.class).getToolchain();
            JavaToolchainService service = extensions.getByType(JavaToolchainService.class);
            Provider<JavaLauncher> defaultLauncher = service.launcherFor(toolchain);
            launcher.convention(defaultLauncher);
        }
    }

    void setExec(JavaExecSpec spec) {
        if (supportsToolchainAndConfigurationCache && launcher.isPresent() && launcher.get() instanceof JavaLauncher) {
            spec.setExecutable(((JavaLauncher) launcher.get()).getExecutablePath().getAsFile().getAbsolutePath());
        } else if (!supportsToolchainAndConfigurationCache && launcher.isPresent()) {
            throw new IllegalArgumentException("Toolchain support requires Gradle " + GRADLE_VERSION_WITH_TOOLCHAIN);
        }
    }

}
