package nu.studer.gradle.jooq;

/**
 * jOOQ comes in different editions, which are published
 * under different group ids. The artifact names and versions
 * are the same for all editions.
 */
public enum JooqEdition {

    OSS("org.jooq"),
    PRO("org.jooq.pro"),
    PRO_JAVA_8("org.jooq.pro-java-8"),
    PRO_JAVA_6("org.jooq.pro-java-6"),
    TRIAL("org.jooq.trial"),
    TRIAL_JAVA_8("org.jooq.trial-java-8"),
    TRIAL_JAVA_6("org.jooq.trial-java-6");

    private final String groupId;

    JooqEdition(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

}
