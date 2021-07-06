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
package nu.studer.gradle.jooq;

/**
 * jOOQ comes in different editions, which are published
 * under different group ids. The artifact names and versions
 * are the same for all editions.
 */
public enum JooqEdition {

    OSS("org.jooq"),
    PRO("org.jooq.pro"),
    PRO_JAVA_11("org.jooq.pro-java-11"),
    PRO_JAVA_8("org.jooq.pro-java-8"),
    PRO_JAVA_6("org.jooq.pro-java-6"),
    TRIAL("org.jooq.trial"),
    TRIAL_JAVA_11("org.jooq.trial-java-11"),
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
