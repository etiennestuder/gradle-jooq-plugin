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

import org.gradle.api.Named;
import org.gradle.api.tasks.SourceSet;
import org.jooq.meta.jaxb.Configuration;

import javax.annotation.Nonnull;

import static java.lang.Character.toUpperCase;

/**
 * Represents a jOOQ configuration which consists of the actual jOOQ source code generation configuration and the source set in which to include the generated sources.
 */
public class JooqConfiguration implements Named {

    private final String name;
    private final SourceSet sourceSet;
    private final Configuration configuration;

    public JooqConfiguration(String name, SourceSet sourceSet, Configuration configuration) {
        this.name = name;
        this.sourceSet = sourceSet;
        this.configuration = configuration;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    public SourceSet getSourceSet() {
        return sourceSet;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public String getJooqTaskName() {
        return "generate" + capitalize(getName()) + "JooqSchemaSource";
    }

    private static String capitalize(String str) {
        return str.length() == 0 ? "" : "" + toUpperCase(str.charAt(0)) + str.subSequence(1, str.length());
    }

}
