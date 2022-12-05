package me.ore.logback.gson.layout;


import com.google.gson.GsonBuilder;


public interface LogbackGsonBuilderConfigurer {
    void configure(GsonBuilder gsonBuilder);
}
