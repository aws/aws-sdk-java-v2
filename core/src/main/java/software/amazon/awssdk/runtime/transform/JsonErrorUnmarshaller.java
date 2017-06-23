/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.runtime.transform;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.PascalCaseStrategy;
import java.lang.reflect.Method;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.annotation.ThreadSafe;

/**
 * Unmarshaller for JSON error responses from AWS services.
 */
@SdkInternalApi
@ThreadSafe
public class JsonErrorUnmarshaller extends AbstractErrorUnmarshaller<JsonNode> {

    public static final JsonErrorUnmarshaller DEFAULT_UNMARSHALLER = new JsonErrorUnmarshaller(
            AmazonServiceException.class, null);

    private static final ObjectMapper MAPPER = new ObjectMapper().configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).setPropertyNamingStrategy(
            new PascalCaseStrategy());

    private final String handledErrorCode;

    /**
     * @param exceptionClass   Exception class this unmarshaller will attempt to deserialize error response into
     * @param handledErrorCode AWS error code that this unmarshaller handles. Pass null to handle all exceptions
     */
    public JsonErrorUnmarshaller(Class<? extends AmazonServiceException> exceptionClass, String handledErrorCode) {
        super(exceptionClass);
        this.handledErrorCode = handledErrorCode;
    }

    @Override
    @ReviewBeforeRelease("Figure out a better way to go from exception class to it's builder class in order to perform the " +
            "deserialization")
    public AmazonServiceException unmarshall(JsonNode jsonContent) throws Exception {
        // FIXME: dirty hack below
        try {
            Method builderClassGetter = exceptionClass.getDeclaredMethod("serializableBuilderClass");
            builderClassGetter.setAccessible(true);
            Class<?> builderClass = (Class<?>) builderClassGetter.invoke(null);
            Method buildMethod = builderClass.getMethod("build");
            buildMethod.setAccessible(true);
            Object o = MAPPER.treeToValue(jsonContent, builderClass);
            return (AmazonServiceException) buildMethod.invoke(o);
        } catch (NoSuchMethodException e) {
            // This exception is not the new style with a builder, assume it's still the old
            // style that we can directly map from JSON
            return MAPPER.treeToValue(jsonContent, exceptionClass);
        }
    }

    /**
     * @param actualErrorCode Actual AWS error code found in the error response.
     * @return True if the actualErrorCode can be handled by this unmarshaller, false otherwise
     */
    public boolean matchErrorCode(String actualErrorCode) {
        if (handledErrorCode == null) {
            return true;
        }
        return handledErrorCode.equals(actualErrorCode);
    }

}
