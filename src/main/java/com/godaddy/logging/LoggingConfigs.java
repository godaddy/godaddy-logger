/**
 * Copyright (c) 2015 GoDaddy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.godaddy.logging;

import com.godaddy.logging.logger.LoggerImpl;
import com.godaddy.logging.logger.MarkerAppendingLogger;
import com.godaddy.logging.messagebuilders.providers.LogstashMessageBuilderProvider;
import com.godaddy.logging.messagebuilders.providers.StringMessageBuilderProvider;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

@Builder
@Data
public class LoggingConfigs {

    @Setter
    @Getter
    private static LoggingConfigs current = builder().build();

    /**
     * The custom mapper provides the ability to pass in a custom toString function for a specific class.
     */
    private final Map<Class<?>, Function<Object, String>> customMapper;

    /**
     * The recursiveLevel defines the number of inner class levels to be logged.
     *
     * Default value is "5".
     */
    private final Integer recursiveLevel;

    /**
     * Methods beginning with one of these defined prefixes will be logged.
     *
     * Default values are "get" and "is".
     */
    private final Set<String> methodPrefixes;

    /**
     * Values that contain these prefixes will not be logged.
     *
     * Default values are "val$" and "this$".
     */
    private final Set<String> excludesPrefixes;

    /**
     * Message builder to format logs.
     *
     * It uses the LoggerMessageBuilder by default.
     */
    private final MessageBuilderProvider<?> messageBuilderFunction;

    /**
     * Processor used to hash data.
     *
     * Using the MD5HashProcessor by default. The MD5HashProcessor uses Guava's MD5 hashing algorithm.
     * MD5 is not cryptographically secure, but it is extremely fast.
     * For a more robust encryption you can use your own HashProcessor.
     */
    private final HashProcessor hashProcessor;

    /**
     * Function to take any exceptions thrown by the logger and translate it to a string
     */
    private final ExceptionTranslator exceptionTranslator;

    /**
     * Function which returns a logger implementation. By default, LoggerImpl is used.
     */
    private final BiFunction<Class<?>, LoggingConfigs, Logger> logger;

    /**
     * Allows the ability to filter collections, if you only want a maximum of 10 elements to be logged per
     * collection you can do so by providing a filter function that will ensure only 10 elements are logged.
     * By default collections are filtered to only contain 50 items.
     */
    private final Function<Collection, Collection> collectionFilter;

    LoggingConfigs(
            Map<Class<?>, Function<Object, String>> customMapper,
            Integer recursiveLevel,
            Set<String> methodPrefixes,
            Set<String> excludesPrefixes,
            MessageBuilderProvider<?> messageBuilderFunction,
            HashProcessor hashProcessor,
            ExceptionTranslator exceptionTranslator,
            BiFunction<Class<?>, LoggingConfigs, Logger> logger,
            Function<Collection, Collection> collectionFilter) {

        this.customMapper = appendDefaultsToCustomMapper(customMapper);

        this.methodPrefixes = appendDefaults(ImmutableSet.of("get", "is"), methodPrefixes);

        this.excludesPrefixes = appendDefaults(ImmutableSet.of("val$", "this$"), excludesPrefixes);

        this.recursiveLevel = recursiveLevel == null ? 5 : recursiveLevel;

        this.logger = logger == null ? (clazz, configs) -> new LoggerImpl(new Slf4WrapperLogger(org.slf4j.LoggerFactory.getLogger(clazz)), configs)
                                     : logger;

        this.messageBuilderFunction = messageBuilderFunction != null ? messageBuilderFunction : new StringMessageBuilderProvider();
        this.hashProcessor = hashProcessor == null ? new Sha256HashProcessor() : hashProcessor;
        this.exceptionTranslator = exceptionTranslator == null ? i -> "<An error occurred logging!>" : exceptionTranslator;
        this.collectionFilter = collectionFilter == null ? collection -> (Collection) collection.stream().limit(50).collect(toList()) : collectionFilter;
    }

    public LoggingConfigs(LoggingConfigs loggingConfigs) {
        this.customMapper = Maps.newHashMap(loggingConfigs.getCustomMapper());
        this.methodPrefixes = Sets.newHashSet(loggingConfigs.getMethodPrefixes());
        this.excludesPrefixes = Sets.newHashSet(loggingConfigs.getExcludesPrefixes());
        this.recursiveLevel = loggingConfigs.getRecursiveLevel();
        this.messageBuilderFunction = loggingConfigs.getMessageBuilderFunction();
        this.hashProcessor = loggingConfigs.getHashProcessor();
        this.logger = loggingConfigs.getLogger();
        this.exceptionTranslator = loggingConfigs.getExceptionTranslator();
        this.collectionFilter = loggingConfigs.getCollectionFilter();
    }

    /**
     * Creates an Immutable copy of Logging Configs with a newly defined Recursive Level.
     *
     * @param recursiveLevel how deep do you want the logging to go for each object
     * @return logging configuration
     */
    public LoggingConfigs withRecursiveLevel(Integer recursiveLevel) {
        return new LoggingConfigs(this.getCustomMapper(),
                                  recursiveLevel,
                                  this.getMethodPrefixes(),
                                  this.getExcludesPrefixes(),
                                  this.getMessageBuilderFunction(),
                                  this.getHashProcessor(),
                                  this.getExceptionTranslator(),
                                  this.logger,
                                  this.getCollectionFilter());
    }

    /**
     * Creates an Immutable copy of Logging Configs with a newly defined Message Builder Function.
     *
     * @param messageBuilderFunction message builder provider
     * @return logging configuration
     */
    public LoggingConfigs withMessageBuilderFunction(MessageBuilderProvider<?> messageBuilderFunction) {
        return new LoggingConfigs(this.getCustomMapper(),
                                  this.getRecursiveLevel(),
                                  this.getMethodPrefixes(),
                                  this.getExcludesPrefixes(),
                                  messageBuilderFunction,
                                  this.getHashProcessor(),
                                  this.getExceptionTranslator(),
                                  this.logger,
                                  this.getCollectionFilter());
    }

    public LoggingConfigs withLogger(BiFunction<Class<?>, LoggingConfigs, Logger> logger) {
        return new LoggingConfigs(this.getCustomMapper(),
                                  this.getRecursiveLevel(),
                                  this.getMethodPrefixes(),
                                  this.getExcludesPrefixes(),
                                  messageBuilderFunction,
                                  this.getHashProcessor(),
                                  this.getExceptionTranslator(),
                                  logger,
                                  this.getCollectionFilter());
    }

    public LoggingConfigs useJson() {
        return new LoggingConfigs(this.getCustomMapper(),
                                  this.getRecursiveLevel(),
                                  this.getMethodPrefixes(),
                                  this.getExcludesPrefixes(),
                                  new LogstashMessageBuilderProvider(),
                                  this.getHashProcessor(),
                                  this.getExceptionTranslator(),
                                  (clazz, configs) -> new MarkerAppendingLogger(new Slf4WrapperLogger(org.slf4j.LoggerFactory.getLogger(clazz)), configs),
                                  this.getCollectionFilter());
    }

    public LoggingConfigs withCollectionFilter(Function<Collection, Collection> collectionFilter) {
        return new LoggingConfigs(this.getCustomMapper(),
                                  this.getRecursiveLevel(),
                                  this.getMethodPrefixes(),
                                  this.getExcludesPrefixes(),
                                  this.getMessageBuilderFunction(),
                                  this.getHashProcessor(),
                                  this.getExceptionTranslator(),
                                  this.logger,
                                  collectionFilter);
    }

    /**
     * Creates an Immutable copy of Logging Configs with a newly defined Hash Processor.
     *
     * @param hashProcessor hash processor
     * @return logging configuration
     */
    public LoggingConfigs withHashProcessor(HashProcessor hashProcessor) {
        return new LoggingConfigs(this.getCustomMapper(),
                                  this.getRecursiveLevel(),
                                  this.getMethodPrefixes(),
                                  this.getExcludesPrefixes(),
                                  this.getMessageBuilderFunction(),
                                  hashProcessor,
                                  this.getExceptionTranslator(),
                                  this.logger,
                                  this.getCollectionFilter());
    }

    /**
     * Creates an Immutable copy of Logging Configs with a newly exception translator
     *
     * @param exceptionTranslator exception translator
     * @return logging configuration
     */
    public LoggingConfigs withExceptionTranslator(ExceptionTranslator exceptionTranslator) {
        return new LoggingConfigs(this.getCustomMapper(),
                                  this.getRecursiveLevel(),
                                  this.getMethodPrefixes(),
                                  this.getExcludesPrefixes(),
                                  this.getMessageBuilderFunction(),
                                  this.getHashProcessor(),
                                  exceptionTranslator,
                                  this.logger,
                                  this.getCollectionFilter());
    }

    /**
     * Ability to add a custom mapping.
     *
     * @param clazz  - class to be mapped to a toString function.
     * @param mapper - toString mapping function for the specified clazz.
     * @param <T>    - type of generic class
     * @return logging configuration
     */
    public <T> LoggingConfigs addOverride(Class<T> clazz, Function<T, String> mapper) {
        customMapper.put(clazz, (Function<Object, String>) mapper);

        return this;
    }

    /**
     * This is deprecated because we don't want any of the with functionality mutating the LoggingConfigs.
     * Use addOverride instead.
     */
    @Deprecated
    public <T> LoggingConfigs withOverride(Class<T> clazz, Function<T, String> mapper) {
        customMapper.put(clazz, (Function<Object, String>) mapper);

        return this;
    }

    private Map<Class<?>, Function<Object, String>> appendDefaultsToCustomMapper(Map<Class<?>, Function<Object, String>> customMapper) {
        Map<Class<?>, Function<Object, String>> defaultMapper = new HashMap<Class<?>, Function<Object, String>>() {{
            put(UUID.class, Object::toString);
        }};

        //Includes ability to override defaults.
        if (customMapper != null) {
            customMapper.keySet().stream()
                        .forEach(clazz -> defaultMapper.put(clazz, customMapper.get(clazz)));
        }

        return defaultMapper;
    }

    private ImmutableSet<String> appendDefaults(ImmutableSet<String> defaults, Set<String> configs) {
        return configs == null ? defaults : new ImmutableSet.Builder<String>().addAll(defaults).addAll(configs).build();
    }

    public Logger getDefaultLogger(Class<?> clazz) {
        return getConfiguredLogger(clazz, LoggingConfigs.getCurrent());
    }

    public Logger getConfiguredLogger(Class<?> clazz, LoggingConfigs configs) {
        return logger.apply(clazz, configs);
    }
}
