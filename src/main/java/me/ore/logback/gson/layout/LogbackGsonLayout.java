package me.ore.logback.gson.layout;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.LayoutBase;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;


@SuppressWarnings("unused")
public class LogbackGsonLayout extends LayoutBase<ILoggingEvent> {
    private static final AtomicLong SEQUENCE = new AtomicLong(Long.MIN_VALUE);

    public static final String CHARSET = "UTF-8";
    public static final String LINE_SEPARATOR = System.lineSeparator();

    public static final String PROPERTY_SEQUENCE_NUMBER = "sequenceNumber";
    public static final String PROPERTY_TIMESTAMP = "timestamp";
    public static final String PROPERTY_THREAD = "thread";
    public static final String PROPERTY_LEVEL = "level";
    public static final String PROPERTY_LOGGER = "logger";
    public static final String PROPERTY_MESSAGE = "message";
    public static final String PROPERTY_ERROR = "error";


    public static final ThreadLocal<Map<String, Object>> EXTRA_PARAMS_HOLDER = new ThreadLocal<>();
    public static void setExtraParams(Map<String, Object> extraParams) { EXTRA_PARAMS_HOLDER.set(extraParams); }
    public static void removeExtraParams() { EXTRA_PARAMS_HOLDER.remove(); }

    public static <T> T useExtraParamsMap(Function<Map<String, Object>, T> usage) {
        try {
            Map<String, Object> map = new HashMap<>();
            EXTRA_PARAMS_HOLDER.set(map);

            return usage.apply(map);
        } finally {
            EXTRA_PARAMS_HOLDER.remove();
        }
    }

    public static void useExtraParamsMap(Consumer<Map<String, Object>> usage) {
        try {
            Map<String, Object> map = new HashMap<>();
            EXTRA_PARAMS_HOLDER.set(map);

            usage.accept(map);
        } finally {
            EXTRA_PARAMS_HOLDER.remove();
        }
    }


    private Charset charset = Charset.forName(CHARSET);
    public void setCharset(String charset) { this.charset = Charset.forName(charset); }
    public String getCharset() { return this.charset.name(); }

    private String lineSeparator = LINE_SEPARATOR;
    public String getLineSeparator() { return lineSeparator; }

    public void setLineSeparator(String lineSeparator) {
        if (lineSeparator.equals("\"")) {
            this.lineSeparator = "";
            return;
        }

        int start = 0;
        if (lineSeparator.startsWith("\"")) start++;

        int end = lineSeparator.length();
        if (lineSeparator.endsWith("\"")) end--;

        this.lineSeparator = lineSeparator.substring(start, end);
    }

    private String propertySequenceNumber = PROPERTY_SEQUENCE_NUMBER;
    public String getPropertySequenceNumber() { return propertySequenceNumber; }
    public void setPropertySequenceNumber(String propertySequenceNumber) { this.propertySequenceNumber = propertySequenceNumber; }

    private String propertyTimestamp = PROPERTY_TIMESTAMP;
    public String getPropertyTimestamp() { return propertyTimestamp; }
    public void setPropertyTimestamp(String propertyTimestamp) { this.propertyTimestamp = propertyTimestamp; }

    private String propertyThread = PROPERTY_THREAD;
    public String getPropertyThread() { return propertyThread; }
    public void setPropertyThread(String propertyThread) { this.propertyThread = propertyThread; }

    private String propertyLevel = PROPERTY_LEVEL;
    public String getPropertyLevel() { return propertyLevel; }
    public void setPropertyLevel(String propertyLevel) { this.propertyLevel = propertyLevel; }

    private String propertyLogger = PROPERTY_LOGGER;
    public String getPropertyLogger() { return propertyLogger; }
    public void setPropertyLogger(String propertyLogger) { this.propertyLogger = propertyLogger; }

    private String propertyMessage = PROPERTY_MESSAGE;
    public String getPropertyMessage() { return propertyMessage; }
    public void setPropertyMessage(String propertyMessage) { this.propertyMessage = propertyMessage; }

    private String propertyError = PROPERTY_ERROR;
    public String getPropertyError() { return propertyError; }
    public void setPropertyError(String propertyError) { this.propertyError = propertyError; }

    private String gsonBuilderConfigurer = null;
    public String getGsonBuilderConfigurer() { return gsonBuilderConfigurer; }
    public void setGsonBuilderConfigurer(String gsonBuilderConfigurer) { this.gsonBuilderConfigurer = gsonBuilderConfigurer; }

    private Gson gson;


    private <T> T valueOrDefault(T value, T defaultValue) { return (value == null ? defaultValue : value); }

    private String readThrowable(IThrowableProxy source) {
        if (source == null) return null;

        StringBuilder resultBuilder = new StringBuilder();
        HashSet<IThrowableProxy> passedThrowables = new HashSet<>();
        boolean first = true;
        IThrowableProxy currentThrowable = source;

        while (currentThrowable != null) {
            if (first) {
                first = false;
            } else {
                resultBuilder.append(this.getLineSeparator()).append("Caused by: ");
            }

            if (passedThrowables.contains(currentThrowable)) {
                resultBuilder.append("[Cyclic chain of throwables detected]");
                break;
            }
            passedThrowables.add(currentThrowable);

            resultBuilder.append(currentThrowable.getClassName()).append(": ").append(currentThrowable.getMessage());

            StackTraceElementProxy[] stackTraceElementProxyArray = currentThrowable.getStackTraceElementProxyArray();
            if (stackTraceElementProxyArray != null) {
                for (StackTraceElementProxy stackTraceElementProxy : stackTraceElementProxyArray) {
                    if (stackTraceElementProxy != null) {
                        resultBuilder
                                .append(this.getLineSeparator())
                                .append("\t")
                                .append(stackTraceElementProxy.getSTEAsString());
                    }
                }
            }

            int commonFrames = currentThrowable.getCommonFrames();
            if (commonFrames > 0) {
                resultBuilder
                        .append(this.getLineSeparator())
                        .append("\t<")
                        .append(commonFrames)
                        .append(" common frame(s) omitted")
                        .append('>');
            }

            currentThrowable = currentThrowable.getCause();
        }

        return resultBuilder.toString();
    }


    @Override
    public void start() {
        super.start();

        String gsonBuilderConfigurerClassName = this.getGsonBuilderConfigurer();

        Class<?> gsonBuilderConfigurerClass = null;
        if (gsonBuilderConfigurerClassName != null) {
            try {
                gsonBuilderConfigurerClass = LogbackGsonLayout.class.getClassLoader().loadClass(gsonBuilderConfigurerClassName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Cannot load class of GSON builder configurer", e);
            }
        }

        Object gsonBuilderConfigurerRaw = null;
        if (gsonBuilderConfigurerClass != null) {
            try {
                gsonBuilderConfigurerRaw = gsonBuilderConfigurerClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Cannot create instance of class \"" + gsonBuilderConfigurerClass.getName() + "\"", e);
            }
        }

        LogbackGsonBuilderConfigurer gsonBuilderConfigurer = null;
        if (gsonBuilderConfigurerRaw != null) {
            try {
                gsonBuilderConfigurer = (LogbackGsonBuilderConfigurer) gsonBuilderConfigurerRaw;
            } catch (Exception e) {
                throw new RuntimeException(
                        "Cannot cast instance of class \"" +
                                gsonBuilderConfigurerRaw.getClass().getName() +
                                "\" to class \"" +
                                LogbackGsonBuilderConfigurer.class.getName() +
                                "\"",
                        e
                );
            }
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        if (gsonBuilderConfigurer != null) gsonBuilderConfigurer.configure(gsonBuilder);
        this.gson = gsonBuilder.create();

        System.out.println("lineSeparator:" + this.lineSeparator.length());
    }

    public String doLayout(ILoggingEvent event) {
        String result;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream, this.charset)) {
                try (JsonWriter jsonWriter = this.gson.newJsonWriter(outputWriter)) {
                    jsonWriter.beginObject();

                    String propertySequenceNumber = this.getPropertySequenceNumber();
                    jsonWriter.name(propertySequenceNumber).value(Long.toHexString(SEQUENCE.getAndIncrement()));

                    String propertyTimestamp = this.getPropertyTimestamp();
                    jsonWriter.name(propertyTimestamp).value(event.getTimeStamp());

                    String propertyThread = this.getPropertyThread();
                    jsonWriter.name(propertyThread).value(this.valueOrDefault(event.getThreadName(), ""));

                    String propertyLevel = this.getPropertyLevel();
                    jsonWriter.name(propertyLevel).value(this.valueOrDefault(event.getLevel(), Level.TRACE).levelStr);

                    String propertyLogger = this.getPropertyLogger();
                    jsonWriter.name(propertyLogger).value(this.valueOrDefault(event.getLoggerName(), ""));

                    String propertyMessage = this.getPropertyMessage();
                    jsonWriter.name(propertyMessage).value(this.valueOrDefault(event.getFormattedMessage(), ""));

                    String propertyError = this.getPropertyError();
                    String throwableText = this.readThrowable(event.getThrowableProxy());
                    if (throwableText != null) {
                        jsonWriter.name(propertyError).value(throwableText);
                    }

                    Map<String, Object> extraParamsMap = EXTRA_PARAMS_HOLDER.get();
                    if (extraParamsMap != null) {
                        for (Map.Entry<String, Object> entry : extraParamsMap.entrySet()) {
                            if (entry == null) continue;

                            String name = entry.getKey();
                            if (name == null) continue;
                            if (name.equals(propertySequenceNumber)) continue;
                            if (name.equals(propertyTimestamp)) continue;
                            if (name.equals(propertyThread)) continue;
                            if (name.equals(propertyLevel)) continue;
                            if (name.equals(propertyLogger)) continue;
                            if (name.equals(propertyMessage)) continue;
                            if (name.equals(propertyError)) continue;

                            Object value = entry.getValue();

                            jsonWriter.name(name);
                            if (value == null) jsonWriter.nullValue();
                            else if (value instanceof String) jsonWriter.value((String) value);
                            else if (value instanceof Double) jsonWriter.value(((Double) value).doubleValue());
                            else if (value instanceof Long) jsonWriter.value(((Long) value).longValue());
                            else if (value instanceof Boolean) jsonWriter.value(((Boolean) value).booleanValue());
                            else if (value instanceof Number) jsonWriter.value((Number) value);
                            else this.gson.toJson(value, value.getClass(), jsonWriter);
                        }
                    }

                    jsonWriter.endObject();

                    jsonWriter.flush();
                    outputWriter.write(this.getLineSeparator());
                    outputWriter.flush();
                }
            }

            byte[] bytes = outputStream.toByteArray();
            result = new String(bytes, this.charset);
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }

        return result;
    }
}
