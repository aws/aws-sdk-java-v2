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

package software.amazon.awssdk.mapper.dynamodb.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.ClassLoaderHelper;

/**
 * Jackson-backed JSON helpers for {@code @DynamoDBTypeConvertedJson}.
 *
 * <p>Jackson is an optional dependency of the mapper. The {@link ObjectMapper} lives in a nested holder class so that
 * loading this class, or calling {@link #isJacksonAvailable()}, does not load any Jackson classes.
 */
@SdkInternalApi
public final class MapperJsonUtils {

    private static final String OBJECT_MAPPER_CLASS = "com.fasterxml.jackson.databind.ObjectMapper";

    private MapperJsonUtils() {
    }

    /**
     * Returns whether Jackson databind is on the classpath.
     */
    public static boolean isJacksonAvailable() {
        try {
            ClassLoaderHelper.loadClass(OBJECT_MAPPER_CLASS, false, MapperJsonUtils.class);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Serializes the given object to a JSON string. Requires Jackson on the classpath.
     */
    public static String writeValueAsString(Object value) throws IOException {
        return ObjectMapperHolder.MAPPER.writeValueAsString(value);
    }

    /**
     * Deserializes the given JSON string to the given type, ignoring unknown properties. Requires Jackson on the
     * classpath.
     */
    public static <T> T readValue(String json, Class<T> type) throws IOException {
        return ObjectMapperHolder.MAPPER.readValue(json, type);
    }

    private static final class ObjectMapperHolder {
        private static final ObjectMapper MAPPER =
            new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
