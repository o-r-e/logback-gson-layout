package me.ore.logback.gson.layout.test;


import com.google.gson.*;
import me.ore.logback.gson.layout.LogbackGsonBuilderConfigurer;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Config implements LogbackGsonBuilderConfigurer {
    public static class DateSerializer implements JsonSerializer<Date> {
        private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) return JsonNull.INSTANCE;
            return context.serialize(FORMAT.format(src));
        }
    }

    @Override
    public void configure(GsonBuilder gsonBuilder) {
        // gsonBuilder.setPrettyPrinting();
        gsonBuilder.registerTypeHierarchyAdapter(Date.class, new DateSerializer());
        gsonBuilder.disableHtmlEscaping();
    }
}
