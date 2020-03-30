package nu.studer.gradle.jooq;

/**
 * jOOQ comes in different editions, which are published
 * under different group ids. The artifact names and versions
 * are the same for all editions.
 */
public enum JooqEdition {

    OSS("org.jooq"),
    PRO("org.jooq.pro"),
    PRO_JAVA_6("org.jooq.pro-java-6"),
    PRO_JAVA_8("org.jooq.pro-java-8"),
    TRIAL("org.jooq.trial");

    private final String groupId;

    JooqEdition(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

}
