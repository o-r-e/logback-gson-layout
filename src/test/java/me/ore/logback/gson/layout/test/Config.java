package me.ore.logback.gson.layout.test;


import com.google.gson.GsonBuilder;
import me.ore.logback.gson.layout.LogbackGsonBuilderConfigurer;

public class Config implements LogbackGsonBuilderConfigurer {
    @Override
    public void configure(GsonBuilder gsonBuilder) {
        // gsonBuilder.setPrettyPrinting();
    }
}
