package nu.studer.sample;

import org.jooq.codegen.DefaultGeneratorStrategy;
import org.jooq.meta.Definition;

public final class SampleGeneratorStrategy extends DefaultGeneratorStrategy {

    @Override
    public String getJavaGetterName(Definition definition, Mode mode) {
        // do not prefix getters with 'get'
        return super.getJavaGetterName(definition, mode).substring("get".length());
    }

}
