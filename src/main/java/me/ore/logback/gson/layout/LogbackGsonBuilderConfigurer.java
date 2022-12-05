package me.ore.logback.gson.layout;


import com.google.gson.GsonBuilder;


/**
 * Interface for objects which can configure instance {@link GsonBuilder}
 */
public interface LogbackGsonBuilderConfigurer {
    /**
     * Configures GSON builder
     *
     * @param gsonBuilder GSON builder to configure
     */
    void configure(GsonBuilder gsonBuilder);
}
