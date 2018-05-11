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

package software.amazon.awssdk.core.protocol.json;

import static com.fasterxml.jackson.databind.PropertyNamingStrategy.UPPER_CAMEL_CASE;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkServiceException;

/**
 * Unmarshaller for JSON error responses from upstream services.
 */
@SdkInternalApi
@ThreadSafe
public class SdkJsonErrorUnmarshaller extends JsonErrorUnmarshaller {

    public static final SdkJsonErrorUnmarshaller DEFAULT_UNMARSHALLER = new SdkJsonErrorUnmarshaller(
        SdkServiceException.class, null);

    private static final ObjectMapper MAPPER = new ObjectMapper().configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).setPropertyNamingStrategy(UPPER_CAMEL_CASE);

    private final Optional<Integer> httpStatusCode;

    /**
     * @param exceptionClass Exception class this unmarshaller will attempt to deserialize error
     *                       response into
     * @param httpStatusCode HTTP status code associated with this modeled exception. A value of
     *                       null will match all http status codes.
     */
    public SdkJsonErrorUnmarshaller(Class<? extends SdkServiceException> exceptionClass, Optional<Integer> httpStatusCode) {
        super(exceptionClass);
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * @param actualHttpStatusCode Actual HTTP status code found in the error response.
     * @return True if the http status can be handled by this unmarshaller, false otherwise
     */
    public boolean matches(int actualHttpStatusCode) {
        return httpStatusCode
            .map(sc -> sc == actualHttpStatusCode)
            .orElse(true);
    }
}
