/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.mapper.dynamodb;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Lazy;

/**
 * Calls the application's jackson-databind for {@link DynamoDBTypeConvertedJson}. The mapper doesn't depend on
 * jackson-databind, so this class finds it on the classpath by name and calls it through reflection.
 *
 * <p>The calls and configuration match the v1 mapper's converter exactly, so the stored JSON is the same: one shared
 * {@code ObjectMapper} with {@code FAIL_ON_UNKNOWN_PROPERTIES} disabled, {@code writeValueAsString(Object)} to write
 * and {@code readValue(String, Class)} to read.
 *
 * <p>This class must not reference any jackson-databind type, so that it can be loaded without jackson-databind.
 */
@SdkInternalApi
final class JacksonDatabindSupport {

    private static final String OBJECT_MAPPER_CLASS = "com.fasterxml.jackson.databind.ObjectMapper";
    private static final String DESERIALIZATION_FEATURE_CLASS = "com.fasterxml.jackson.databind.DeserializationFeature";

    /**
     * Checked with the mapper's own class loader, for the same reason as {@link JodaTimeSupport}.
     */
    private static final boolean AVAILABLE = isLoadable(OBJECT_MAPPER_CLASS);

    private static final Lazy<ObjectMapperBridge> BRIDGE = new Lazy<>(ObjectMapperBridge::create);

    private JacksonDatabindSupport() {
    }

    /**
     * Returns whether jackson-databind is on the mapper's classpath.
     */
    static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * Returns the exception to throw when a feature that needs jackson-databind is used without it.
     *
     * @param feature the feature that needs jackson-databind, for example {@code "@DynamoDBTypeConvertedJson"}
     */
    static DynamoDBMappingException missing(String feature) {
        return new DynamoDBMappingException(
            feature + " requires jackson-databind, which the mapper doesn't include. Add "
            + "com.fasterxml.jackson.core:jackson-databind (Brazil package Jackson-databind) to the classpath, where "
            + "the mapper's class loader can load it.");
    }

    /**
     * Prepares the shared {@code ObjectMapper}, so that a missing or unusable jackson-databind fails when the mapper
     * builds the model rather than on the first save or load.
     */
    static void initialize() {
        BRIDGE.getValue();
    }

    /**
     * Writes {@code value} as JSON, as {@code ObjectMapper.writeValueAsString} does.
     *
     * @throws Exception the exception jackson-databind threw
     */
    static String writeValueAsString(Object value) throws Exception {
        ObjectMapperBridge bridge = BRIDGE.getValue();
        return (String) invoke(bridge.writeValueAsString, bridge.objectMapper, value);
    }

    /**
     * Reads {@code json} as {@code type}, as {@code ObjectMapper.readValue(String, Class)} does.
     *
     * @throws Exception the exception jackson-databind threw
     */
    @SuppressWarnings("unchecked")
    static <T> T readValue(String json, Class<T> type) throws Exception {
        ObjectMapperBridge bridge = BRIDGE.getValue();
        return (T) invoke(bridge.readValue, bridge.objectMapper, json, type);
    }

    /**
     * Calls {@code method} and rethrows what it threw, rather than an {@link InvocationTargetException}, so callers see
     * the same exceptions as a direct call.
     */
    private static Object invoke(Method method, Object target, Object... args) throws Exception {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw e;
        }
    }

    private static boolean isLoadable(String className) {
        try {
            Class.forName(className, false, JacksonDatabindSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static final class ObjectMapperBridge {
        private final Object objectMapper;
        private final Method writeValueAsString;
        private final Method readValue;

        private ObjectMapperBridge(Object objectMapper, Method writeValueAsString, Method readValue) {
            this.objectMapper = objectMapper;
            this.writeValueAsString = writeValueAsString;
            this.readValue = readValue;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static ObjectMapperBridge create() {
            if (!AVAILABLE) {
                throw missing("@DynamoDBTypeConvertedJson");
            }
            try {
                ClassLoader loader = JacksonDatabindSupport.class.getClassLoader();
                Class<?> mapperClass = Class.forName(OBJECT_MAPPER_CLASS, true, loader);
                Class<? extends Enum> featureClass =
                    (Class<? extends Enum>) Class.forName(DESERIALIZATION_FEATURE_CLASS, true, loader);

                Object objectMapper = mapperClass.getConstructor().newInstance();
                mapperClass.getMethod("disable", featureClass)
                           .invoke(objectMapper, Enum.valueOf(featureClass, "FAIL_ON_UNKNOWN_PROPERTIES"));

                return new ObjectMapperBridge(objectMapper,
                                              mapperClass.getMethod("writeValueAsString", Object.class),
                                              mapperClass.getMethod("readValue", String.class, Class.class));
            } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
                throw new DynamoDBMappingException(
                    "@DynamoDBTypeConvertedJson found jackson-databind on the classpath but couldn't use it. It "
                    + "requires jackson-databind 2.x.", e);
            }
        }
    }
}
