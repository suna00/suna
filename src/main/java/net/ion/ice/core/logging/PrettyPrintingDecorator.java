package net.ion.ice.core.logging;

import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.decorate.JsonGeneratorDecorator;

/**
 * Created by jaehocho on 2017. 3. 6..
 */

public class PrettyPrintingDecorator implements JsonGeneratorDecorator {
    @Override
    public JsonGenerator decorate(JsonGenerator generator) {
        return generator.useDefaultPrettyPrinter();
    }
}

