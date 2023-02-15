package me.ore.logback.gson.layout.test;


import me.ore.logback.gson.layout.LogbackGsonLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        int loopCount = 50;
        for (int i = 0; i < loopCount; i++) {
            LogbackGsonLayout.useExtraParamsMap(map -> {
                map.put("userId", "3af2fbd7-ab40-4803-aaa1-1330467184dd");
                map.put("processKey", "Module:generating-report");
                map.put("processId", "8374656874");

                HashMap<String, Object> nestedMap = new HashMap<>();
                nestedMap.put("string", "My text");
                nestedMap.put("number", 123.6);
                nestedMap.put("date", new Date());
                nestedMap.put("boolean", true);
                map.put("nested", nestedMap);

                LOGGER.debug("Debug message");
                LOGGER.info("Info message");

                try {
                    LOGGER.warn("Prepare to exception!");
                    throw new IllegalStateException("This is invalid state");
                } catch (Exception error) {
                    Exception wrappingError = new RuntimeException("Wrapping error", error);
                    LOGGER.error("We have problem", wrappingError);
                }
            });
        }

        System.out.println("----");

        LOGGER.info("", new RuntimeException("Test"));
    }
}
