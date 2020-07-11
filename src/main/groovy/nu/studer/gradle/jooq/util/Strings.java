package nu.studer.gradle.jooq.util;

public final class Strings {

    public static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private Strings() {
    }

}
