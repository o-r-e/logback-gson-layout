package me.ore.logback.gson.layout;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.LayoutBase;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Instance of this layout class converts every log event to JSON object
 */
@SuppressWarnings("unused")
public class LogbackGsonLayout extends LayoutBase<ILoggingEvent> {
    // region Class constants
    private static final AtomicLong SEQUENCE = new AtomicLong(Long.MIN_VALUE);


    /**
     * Default value for line separator which separates JSON objects of log events
     *
     * <p>System default line separator</p>
     *
     * @see #getLogLineSeparator()
     * @see #setLogLineSeparator(String)
     */
    public static final String LOG_LINE_SEPARATOR = System.lineSeparator();

    /**
     * Default value for line separator which separates lines of error stack trace
     *
     * @see #getStackTraceLineSeparator()
     * @see #setStackTraceLineSeparator(String)
     */
    public static final String STACK_TRACE_LINE_SEPARATOR = "\r\n";

    /**
     * Default name for &quot;sequence number&quot; property
     *
     * <p>
     * &quot;Sequence number&quot; - number of log event, unique in whole application since application started
     * </p>
     *
     * @see #getPropertySequenceNumber()
     * @see #setPropertySequenceNumber(String)
     */
    public static final String PROPERTY_SEQUENCE_NUMBER = "sequenceNumber";

    /**
     * Default name for &quot;timestamp&quot; property
     *
     * <p>
     * &quot;Timestamp&quot; - date and time when log event has been created
     * </p>
     *
     * @see #getPropertyTimestamp()
     * @see #setPropertyTimestamp(String)
     */
    public static final String PROPERTY_TIMESTAMP = "timestamp";

    /**
     * Default name for &quot;thread&quot; property
     *
     * <p>
     * &quot;Thread&quot; - name of thread in which log event has been created
     * </p>
     *
     * @see #getPropertyThread()
     * @see #setPropertyThread(String)
     */
    public static final String PROPERTY_THREAD = "thread";

    /**
     * Default name for &quot;level&quot; property
     *
     * <p>
     * &quot;Level&quot; - level of log event (INFO, ERROR and so on)
     * </p>
     *
     * @see #getPropertyLevel()
     * @see #setPropertyLevel(String)
     */
    public static final String PROPERTY_LEVEL = "level";

    /**
     * Default name for &quot;logger&quot; property
     *
     * <p>
     * &quot;Logger&quot; - name of logger which created log event
     * </p>
     *
     * @see #getPropertyLogger()
     * @see #setPropertyLogger(String)
     */
    public static final String PROPERTY_LOGGER = "logger";

    /**
     * Default name for &quot;message&quot; property
     *
     * <p>
     * &quot;Message&quot; - message of log event
     * </p>
     *
     * @see #getPropertyMessage()
     * @see #setPropertyMessage(String)
     */
    public static final String PROPERTY_MESSAGE = "message";

    /**
     * Default name for &quot;error&quot; property
     *
     * <p>
     * &quot;Error&quot; - stacktrace of error in log event (if exists)
     * </p>
     *
     * @see #getPropertyError()
     * @see #setPropertyError(String)
     */
    public static final String PROPERTY_ERROR = "error";
    // endregion


    // region Extra params
    /**
     * Holder for extra parameters (additional values) which cannot be got from log event
     *
     * <p>
     *     If this holder has map during logs' generation, this map will be used as source of additional JSON fields
     * </p>
     *
     * @see #setExtraParams(Map)
     * @see #removeExtraParams()
     * @see #useExtraParamsMap(Consumer)
     * @see #useExtraParamsMap(Function)
     */
    public static final ThreadLocal<Map<String, Object>> EXTRA_PARAMS_HOLDER = new ThreadLocal<>();

    /**
     * Saves `extraParams` to {@link #EXTRA_PARAMS_HOLDER}
     *
     * @param extraParams extra parameters (additional values) which cannot be got from log event
     *
     * @see #EXTRA_PARAMS_HOLDER
     * @see #removeExtraParams()
     * @see #useExtraParamsMap(Consumer)
     * @see #useExtraParamsMap(Function)
     */
    public static void setExtraParams(Map<String, Object> extraParams) { EXTRA_PARAMS_HOLDER.set(extraParams); }

    /**
     * Clears {@link #EXTRA_PARAMS_HOLDER}
     *
     * @see #EXTRA_PARAMS_HOLDER
     * @see #setExtraParams(Map)
     * @see #useExtraParamsMap(Consumer)
     * @see #useExtraParamsMap(Function)
     */
    public static void removeExtraParams() { EXTRA_PARAMS_HOLDER.remove(); }

    /**
     * Creates and saves new {@link HashMap} in {@link #EXTRA_PARAMS_HOLDER}; after sends this map to `usage` function
     *
     * <p>
     * Inside `usage` - created map can be filled, after logs can be generated
     * </p>
     *
     * @param usage function, typically - logging
     * @return result of call of `usage`
     * @param <T> return type of `usage`
     *
     * @see #EXTRA_PARAMS_HOLDER
     * @see #setExtraParams(Map)
     * @see #removeExtraParams()
     * @see #useExtraParamsMap(Consumer)
     */
    public static <T> T useExtraParamsMap(Function<Map<String, Object>, T> usage) {
        try {
            Map<String, Object> map = new HashMap<>();
            EXTRA_PARAMS_HOLDER.set(map);

            return usage.apply(map);
        } finally {
            EXTRA_PARAMS_HOLDER.remove();
        }
    }

    /**
     * Creates and saves new {@link HashMap} in {@link #EXTRA_PARAMS_HOLDER}; after sends this map to `usage` function
     *
     * <p>
     * Inside `usage` - created map can be filled, after logs can be generated
     * </p>
     *
     * @param usage function, typically - logging
     *
     * @see #EXTRA_PARAMS_HOLDER
     * @see #setExtraParams(Map)
     * @see #removeExtraParams()
     * @see #useExtraParamsMap(Function)
     */
    public static void useExtraParamsMap(Consumer<Map<String, Object>> usage) {
        try {
            Map<String, Object> map = new HashMap<>();
            EXTRA_PARAMS_HOLDER.set(map);

            usage.accept(map);
        } finally {
            EXTRA_PARAMS_HOLDER.remove();
        }
    }
    // endregion


    // region Utils
    private static <T> T valueOrDefault(T value, T defaultValue) { return (value == null ? defaultValue : value); }

    private static String readThrowable(String stackTraceLineSeparator, IThrowableProxy source) {
        if (source == null) return null;

        StringBuilder resultBuilder = new StringBuilder();
        HashSet<IThrowableProxy> passedThrowables = new HashSet<>();
        boolean first = true;
        IThrowableProxy currentThrowable = source;

        while (currentThrowable != null) {
            if (first) {
                first = false;
            } else {
                resultBuilder.append(stackTraceLineSeparator).append("Caused by: ");
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
                                .append(stackTraceLineSeparator)
                                .append("\t")
                                .append(stackTraceElementProxy.getSTEAsString());
                    }
                }
            }

            int commonFrames = currentThrowable.getCommonFrames();
            if (commonFrames > 0) {
                resultBuilder
                        .append(stackTraceLineSeparator)
                        .append("\t<")
                        .append(commonFrames)
                        .append(" common frame(s) omitted")
                        .append('>');
            }

            currentThrowable = currentThrowable.getCause();
        }

        return resultBuilder.toString();
    }

    private static String readPossibleQuoted(String source) {
        if (source.equals("\"")) return "";

        int start = 0;
        if (source.startsWith("\"")) start++;

        int end = source.length();
        if (source.endsWith("\"")) end--;

        return source.substring(start, end);
    }
    // endregion


    // region Instance properties
    private String logLineSeparator = LOG_LINE_SEPARATOR;

    /**
     * @return line separator which separates JSON objects of log events; by default - {@link #LOG_LINE_SEPARATOR}
     *
     * @see #LOG_LINE_SEPARATOR
     * @see #setLogLineSeparator(String)
     */
    public String getLogLineSeparator() { return logLineSeparator; }

    /**
     * Sets line separator which separates JSON objects of log events
     *
     * <p>
     *     If first character or last character new {@code logLineSeparator} are «{@code "}», those characters will be ignored
     * </p>
     *
     * @param logLineSeparator new log line separator
     *
     * @see #LOG_LINE_SEPARATOR
     * @see #getLogLineSeparator()
     */
    public void setLogLineSeparator(String logLineSeparator) { this.logLineSeparator = readPossibleQuoted(logLineSeparator); }


    private String stackTraceLineSeparator = STACK_TRACE_LINE_SEPARATOR;

    /**
     * @return line separator which separates lines of error stack trace; by default - {@link #STACK_TRACE_LINE_SEPARATOR}
     *
     * @see #STACK_TRACE_LINE_SEPARATOR
     * @see #setStackTraceLineSeparator(String)
     */
    public String getStackTraceLineSeparator() { return stackTraceLineSeparator; }

    /**
     * Sets line separator which separates lines of error stack trace
     *
     * <p>
     *     If first character or last character of new {@code stackTraceLineSeparator} are «{@code "}», those characters will be ignored
     * </p>
     *
     * @param stackTraceLineSeparator new log line separator
     *
     * @see #STACK_TRACE_LINE_SEPARATOR
     * @see #getStackTraceLineSeparator()
     */
    public void setStackTraceLineSeparator(String stackTraceLineSeparator) { this.stackTraceLineSeparator = readPossibleQuoted(stackTraceLineSeparator); }


    private String propertySequenceNumber = PROPERTY_SEQUENCE_NUMBER;

    /**
     * Returns name for &quot;sequence number&quot; property
     *
     * <p>
     *     &quot;Sequence number&quot; - number of log event, unique in whole application since application started
     * </p>
     *
     * @return property name
     *
     * @see #PROPERTY_SEQUENCE_NUMBER
     * @see #setPropertySequenceNumber(String)
     */
    public String getPropertySequenceNumber() { return propertySequenceNumber; }

    /**
     * Sets name for &quot;sequence number&quot; property
     *
     * <p>
     *     &quot;Sequence number&quot; - number of log event, unique in whole application since application started
     * </p>
     *
     * @param propertySequenceNumber new property name
     *
     * @see #PROPERTY_SEQUENCE_NUMBER
     * @see #getPropertySequenceNumber()
     */
    public void setPropertySequenceNumber(String propertySequenceNumber) { this.propertySequenceNumber = propertySequenceNumber; }


    private String propertyTimestamp = PROPERTY_TIMESTAMP;

    /**
     * Gets name for &quot;timestamp&quot; property
     *
     * <p>
     * &quot;Timestamp&quot; - date and time when log event has been created
     * </p>
     *
     * @return property name
     *
     * @see #PROPERTY_TIMESTAMP
     * @see #setPropertyTimestamp(String)
     */
    public String getPropertyTimestamp() { return propertyTimestamp; }

    /**
     * Sets name for &quot;timestamp&quot; property
     *
     * <p>
     * &quot;Timestamp&quot; - date and time when log event has been created
     * </p>
     *
     * @param propertyTimestamp new property name
     *
     * @see #PROPERTY_TIMESTAMP
     * @see #getPropertyTimestamp()
     */
    public void setPropertyTimestamp(String propertyTimestamp) { this.propertyTimestamp = propertyTimestamp; }


    private String propertyThread = PROPERTY_THREAD;

    /**
     * Gets name for &quot;thread&quot; property
     *
     * <p>
     * &quot;Thread&quot; - name of thread in which log event has been created
     * </p>
     *
     * @return property name
     *
     * @see #PROPERTY_THREAD
     * @see #setPropertyThread(String)
     */
    public String getPropertyThread() { return propertyThread; }

    /**
     * Sets name for &quot;thread&quot; property
     *
     * <p>
     * &quot;Thread&quot; - name of thread in which log event has been created
     * </p>
     *
     * @param propertyThread new property name
     *
     * @see #PROPERTY_THREAD
     * @see #getPropertyThread()
     */
    public void setPropertyThread(String propertyThread) { this.propertyThread = propertyThread; }


    private String propertyLevel = PROPERTY_LEVEL;

    /**
     * Gets name for &quot;level&quot; property
     *
     * <p>
     * &quot;Level&quot; - level of log event (INFO, ERROR and so on)
     * </p>
     *
     * @return property name
     *
     * @see #PROPERTY_LEVEL
     * @see #setPropertyLevel(String)
     */
    public String getPropertyLevel() { return propertyLevel; }

    /**
     * Sets name for &quot;level&quot; property
     *
     * <p>
     * &quot;Level&quot; - level of log event (INFO, ERROR and so on)
     * </p>
     *
     * @param propertyLevel new property name
     *
     * @see #PROPERTY_LEVEL
     * @see #getPropertyLevel()
     */
    public void setPropertyLevel(String propertyLevel) { this.propertyLevel = propertyLevel; }


    private String propertyLogger = PROPERTY_LOGGER;

    /**
     * Gets name for &quot;logger&quot; property
     *
     * <p>
     * &quot;Logger&quot; - name of logger which created log event
     * </p>
     *
     * @return property name
     *
     * @see #PROPERTY_LOGGER
     * @see #setPropertyLogger(String)
     */
    public String getPropertyLogger() { return propertyLogger; }

    /**
     * Sets name for &quot;logger&quot; property
     *
     * <p>
     * &quot;Logger&quot; - name of logger which created log event
     * </p>
     *
     * @param propertyLogger new property name
     *
     * @see #PROPERTY_LOGGER
     * @see #getPropertyLogger()
     */
    public void setPropertyLogger(String propertyLogger) { this.propertyLogger = propertyLogger; }


    private String propertyMessage = PROPERTY_MESSAGE;

    /**
     * Gets name for &quot;message&quot; property
     *
     * <p>
     * &quot;Message&quot; - message of log event
     * </p>
     *
     * @return property name
     *
     * @see #PROPERTY_MESSAGE
     * @see #setPropertyMessage(String)
     */
    public String getPropertyMessage() { return propertyMessage; }

    /**
     * Sets name for &quot;message&quot; property
     *
     * <p>
     * &quot;Message&quot; - message of log event
     * </p>
     *
     * @param propertyMessage new property name
     *
     * @see #PROPERTY_MESSAGE
     * @see #getPropertyMessage()
     */
    public void setPropertyMessage(String propertyMessage) { this.propertyMessage = propertyMessage; }


    private String propertyError = PROPERTY_ERROR;

    /**
     * Gets name for &quot;error&quot; property
     *
     * <p>
     * &quot;Error&quot; - stacktrace of error in log event (if exists)
     * </p>
     *
     * @return property name
     *
     * @see #PROPERTY_ERROR
     * @see #setPropertyError(String)
     */
    public String getPropertyError() { return propertyError; }

    /**
     * Sets name for &quot;error&quot; property
     *
     * <p>
     * &quot;Error&quot; - stacktrace of error in log event (if exists)
     * </p>
     *
     * @param propertyError new property name
     *
     * @see #PROPERTY_ERROR
     * @see #setPropertyError(String)
     */
    public void setPropertyError(String propertyError) { this.propertyError = propertyError; }


    private String gsonBuilderConfigurer = null;

    /**
     * @return Name of class which implements interface {@link LogbackGsonBuilderConfigurer}
     *
     * @see #setGsonBuilderConfigurer(String)
     */
    public String getGsonBuilderConfigurer() { return gsonBuilderConfigurer; }

    /**
     * Sets name of class which implements interface {@link LogbackGsonBuilderConfigurer}
     *
     * @param gsonBuilderConfigurer class name
     *
     * @see #getGsonBuilderConfigurer()
     */
    public void setGsonBuilderConfigurer(String gsonBuilderConfigurer) { this.gsonBuilderConfigurer = gsonBuilderConfigurer; }

    private Gson gson;
    // endregion


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
    }

    public String doLayout(ILoggingEvent event) {
        String result;
        try (StringWriter stringWriter = new StringWriter(256)) {
            try (JsonWriter jsonWriter = this.gson.newJsonWriter(stringWriter)) {
                jsonWriter.beginObject();

                String propertySequenceNumber = this.getPropertySequenceNumber();
                jsonWriter.name(propertySequenceNumber).value(Long.toHexString(SEQUENCE.getAndIncrement()));

                String propertyTimestamp = this.getPropertyTimestamp();
                jsonWriter.name(propertyTimestamp).value(event.getTimeStamp());

                String propertyThread = this.getPropertyThread();
                jsonWriter.name(propertyThread).value(valueOrDefault(event.getThreadName(), ""));

                String propertyLevel = this.getPropertyLevel();
                jsonWriter.name(propertyLevel).value(valueOrDefault(event.getLevel(), Level.TRACE).levelStr);

                String propertyLogger = this.getPropertyLogger();
                jsonWriter.name(propertyLogger).value(valueOrDefault(event.getLoggerName(), ""));

                String propertyMessage = this.getPropertyMessage();
                jsonWriter.name(propertyMessage).value(valueOrDefault(event.getFormattedMessage(), ""));

                String propertyError = this.getPropertyError();
                String throwableText = readThrowable(this.getStackTraceLineSeparator(), event.getThrowableProxy());
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
                stringWriter.write(this.getLogLineSeparator());
                stringWriter.flush();
            }

            result = stringWriter.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }

        return result;
    }
}
