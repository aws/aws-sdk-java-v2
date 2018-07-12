/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.internal.protocol.json;

import static com.fasterxml.jackson.databind.PropertyNamingStrategy.UPPER_CAMEL_CASE;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.runtime.transform.AbstractErrorUnmarshaller;


@SdkInternalApi
public abstract class JsonErrorUnmarshaller<T extends SdkServiceException> extends AbstractErrorUnmarshaller<T, JsonNode> {

    private static final ObjectMapper MAPPER = new ObjectMapper().configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setPropertyNamingStrategy(UPPER_CAMEL_CASE);

    protected JsonErrorUnmarshaller(Class<? extends SdkServiceException> exceptionClass) {
        super(exceptionClass);
    }

    @Override
    @ReviewBeforeRelease("Figure out a better way to go from exception class to it's builder class in order to perform the " +
                         "deserialization.")
    @SuppressWarnings("unchecked")
    public T unmarshall(JsonNode jsonContent) throws Exception {
        // FIXME: Generate Unmarshallers for each type instead of using reflection
        try {
            Method builderClassGetter = exceptionClass.getDeclaredMethod("serializableBuilderClass");
            makeAccessible(builderClassGetter);
            Class<?> builderClass = (Class<?>) builderClassGetter.invoke(null);
            Method buildMethod = builderClass.getMethod("build");
            makeAccessible(buildMethod);
            Object o = MAPPER.treeToValue(jsonContent, builderClass);
            return (T) buildMethod.invoke(o);
        } catch (NoSuchMethodException e) {
            // This exception is not the new style with a builder, assume it's still the old
            // style that we can directly map from JSON
            return  (T) MAPPER.treeToValue(jsonContent, exceptionClass);
        }
    }
}
