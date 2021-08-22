package nu.studer.gradle.jooq.util;

import org.gradle.util.GradleVersion;

public final class Gradles {

    public static boolean isAtLeastGradleVersion(String version) {
        return GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version(version)) >= 0;
    }

    private Gradles() {
    }

}
