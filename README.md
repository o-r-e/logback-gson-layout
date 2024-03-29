# logback-gson-layout
Layout for Logback, which converts log events to JSON objects using GSON

## Tests?

Note: directory
[tests](https://github.com/o-r-e/logback-gson-layout/tree/master/src/test)
does not contain any unit tests :) .

## What this library does

This library contains layout for [Logback](https://logback.qos.ch/index.html),
see [more about layouts](https://logback.qos.ch/manual/layouts.html);
this layout converts every log event to JSON object like this one
(but in one line - if without some configuration):

```json
{
    "sequenceNumber": "8000000000000003",
    "timestamp": 1670263878164,
    "thread": "main",
    "level": "ERROR",
    "logger": "me.ore.logback.gson.layout.test.Main",
    "message": "We have problem",
    "error": "java.lang.RuntimeException: Wrapping error\r\n\tat me.ore.logback.gson.layout.test.Main.lambda$main$0(Main.java:36)\r\n\tat me.ore.logback.gson.layout.LogbackGsonLayout.useExtraParamsMap(LogbackGsonLayout.java:62)\r\n\tat me.ore.logback.gson.layout.test.Main.main(Main.java:17)\r\nCaused by: java.lang.IllegalStateException: This is invalid state\r\n\tat me.ore.logback.gson.layout.test.Main.lambda$main$0(Main.java:34)\r\n\tat me.ore.logback.gson.layout.LogbackGsonLayout.useExtraParamsMap(LogbackGsonLayout.java:62)\r\n\tat me.ore.logback.gson.layout.test.Main.main(Main.java:17)\r\n\t\u003c2 common frame(s) omitted\u003e",
    "processId": "8374656874",
    "processKey": "Module:generating-report",
    "userId": "3af2fbd7-ab40-4803-aaa1-1330467184dd",
    "nested": {
        "date": "2022-12-06T00:11:18.152+06:00",
        "number": 123.6,
        "boolean": true,
        "string": "My text"
    }
}
```

JSON object above generated by code in class
[me.ore.logback.gson.layout.test.Main](https://github.com/o-r-e/logback-gson-layout/blob/master/src/test/java/me/ore/logback/gson/layout/test/Main.java).

There are some configurations, see below.

## Download

Download as maven dependency:

```xml
<dependency>
  <groupId>me.o-r-e</groupId>
  <artifactId>logback-gson-layout</artifactId>
  <version>0.0.3</version>
</dependency>
```

Binaries and other examples can be found at https://search.maven.org/artifact/me.o-r-e/logback-gson-layout/0.0.3/jar.

## How to use

### Logback configuration

In appender configuration (typically - for file appender) use encoder of class
`ch.qos.logback.core.encoder.LayoutWrappingEncoder`
and layout of class
`me.ore.logback.gson.layout.LogbackGsonLayout`.
Minimal configuration for console appender looks like this:

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
        <layout class="me.ore.logback.gson.layout.LogbackGsonLayout"/>
    </encoder>
</appender>
```

There are some configuration parameters can be applied inside
`<layout class="me.ore.logback.gson.layout.LogbackGsonLayout"></layout>`,
for example:

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
        <layout class="me.ore.logback.gson.layout.LogbackGsonLayout">
            <logLineSeparator>&quot;&#0013;&#0010;&quot;</logLineSeparator>
            <stackTraceLineSeparator>&quot;&#0013;&#0010;&quot;</stackTraceLineSeparator>
            <propertySequenceNumber>sequenceNumber</propertySequenceNumber>
            <propertyTimestamp>timestamp</propertyTimestamp>
            <propertyThread>thread</propertyThread>
            <propertyLevel>level</propertyLevel>
            <propertyLogger>logger</propertyLogger>
            <propertyMessage>message</propertyMessage>
            <propertyError>error</propertyError>
            <defaultMessage>[NO-MSG]</defaultMessage>
            <gsonBuilderConfigurer>me.ore.logback.gson.layout.test.Config</gsonBuilderConfigurer>
        </layout>
    </encoder>
</appender>
```

Configuration parameters:

<table>
<tr>
<th>Name</th>
<th>Default value</th>
<th>Description</th>
</tr>

<tr>
<td>logLineSeparator</td>
<td>
Default system line separator, result of
<a href="https://docs.oracle.com/javase/8/docs/api/java/lang/System.html#lineSeparator--">
    java.lang.System.lineSeparator()
</a>
</td>
<td>
This line separator used to separate event records from each other.
</td>
</tr>

<tr>
<td>stackTraceLineSeparator</td>
<td>
<code>&quot;\r\n&quot;</code>
</td>
<td>
This line separator which separates lines of error stack trace.
</td>
</tr>

<tr>
<td>propertySequenceNumber</td>
<td><code>&quot;sequenceNumber&quot;</code></td>
<td rowspan="7">Set one or more of these properties to rename JSON fields</td>
</tr>
<tr>
<td>propertyTimestamp</td>
<td><code>&quot;timestamp&quot;</code></td>
</tr>
<tr>
<td>propertyThread</td>
<td><code>&quot;thread&quot;</code></td>
</tr>
<tr>
<td>propertyLevel</td>
<td><code>&quot;level&quot;</code></td>
</tr>
<tr>
<td>propertyLogger</td>
<td><code>&quot;logger&quot;</code></td>
</tr>
<tr>
<td>propertyMessage</td>
<td><code>&quot;message&quot;</code></td>
</tr>
<tr>
<td>propertyError</td>
<td><code>&quot;error&quot;</code></td>
</tr>
<tr>
<td>defaultMessage</td>
<td><code>&quot;&quot;</code></td>
<td>This text will replace blank log event message. &quot;Blank&quot; - empty message or message with space characters only.</td>
</tr>

<tr>
<td>gsonBuilderConfigurer</td>
<td></td>
<td>
    Class which implements
    <a href="https://github.com/o-r-e/logback-gson-layout/blob/master/src/main/java/me/ore/logback/gson/layout/LogbackGsonBuilderConfigurer.java">
        me.ore.logback.gson.layout.LogbackGsonBuilderConfigurer
    </a>.
    <br>
    In this class you can configure instance of class
    <a href="https://github.com/google/gson/blob/master/gson/src/main/java/com/google/gson/GsonBuilder.java">
        com.google.gson.GsonBuilder
    </a>.
    <br>
    This instance of <code>com.google.gson.GsonBuilder</code> used to create instance of class
    <a href="https://github.com/google/gson/blob/master/gson/src/main/java/com/google/gson/Gson.java">
        com.google.gson.Gson
    </a>.
    <br>
    Created instance of <code>com.google.gson.Gson</code> used to convert log event to JSON object.
    <br>
    In implementation of <code>me.ore.logback.gson.layout.LogbackGsonBuilderConfigurer</code>
    you can set pretty printing, add type adapters, and so on.
    Example can be found
    <a href="https://github.com/o-r-e/logback-gson-layout/blob/master/src/test/java/me/ore/logback/gson/layout/test/Config.java">here</a>
    .
</td>
</tr>
</table>


### Extra fields

You can add extra fields in event JSON object;
for example - fields `processId`, `processKey`, `userId` and `nested`
in example in section **"What this library does"** above have been added in that way.

Simplest way - use one of `LogbackGsonLayout.useExtraParamsMap` methods:

```java
class MyClass {
    public void myLogging() {
        LogbackGsonLayout.useExtraParamsMap(map -> {
            map.put("userId", "3af2fbd7-ab40-4803-aaa1-1330467184dd");
            map.put("processKey", "Module:generating-report");
            map.put("processId", "8374656874");

            LOGGER.info("Info message");
        });
    }
}
```

Another way - work directly with holder of such extra parameters map:

```java
class MyClass {
    public void myLogging() {
        try {
            HashMap<String, Object> map = new HashMap<>();

            LogbackGsonLayout.setExtraParams(map);
            // OR
            LogbackGsonLayout.EXTRA_PARAMS_HOLDER.set(map);

            map.put("userId", "3af2fbd7-ab40-4803-aaa1-1330467184dd");

            LOGGER.info("Info message");
        } finally {
            LogbackGsonLayout.removeExtraParams();
            // OR
            LogbackGsonLayout.EXTRA_PARAMS_HOLDER.remove();
        }
    }
}
```

**Note**:

* if name (key) of extra parameter overlaps with "main" JSON fields, such parameter will be silently skipped;
  "main" JSON fields described in table **"Configuration parameters"** in section **"Logback configuration"** above;
* if extra parameter value will not be `null`, `java.lang.String`, 'java.lang.Number' or `java.lang.Boolean`,
  then JSON serializer (<code>com.google.gson.Gson</code>) will use type adapters and/or java reflection,
  which can slow down logs' generation.

Block `finally` is mostly optional - because

> after a thread goes away, all of its copies of thread-local instances are subject to garbage collection
> (unless other references to these copies exist)

(found at https://docs.oracle.com/javase/8/docs/api/java/lang/ThreadLocal.html)
