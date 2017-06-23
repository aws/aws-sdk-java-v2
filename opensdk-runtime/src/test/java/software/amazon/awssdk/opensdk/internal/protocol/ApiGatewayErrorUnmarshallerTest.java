/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.opensdk.internal.protocol;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.opensdk.SdkErrorHttpMetadata;
import software.amazon.awssdk.opensdk.internal.BaseException;

public class ApiGatewayErrorUnmarshallerTest {

    private final ApiGatewayErrorUnmarshaller unmarshaller = new ApiGatewayErrorUnmarshaller(
            ModeledException.class, Optional.of(423));

    @Test
    public void matchingErrorCode_ReturnsTrueForMatches() {
        assertTrue(unmarshaller.matches(423));
    }

    @Test
    public void nonMatchingErrorCode_ReturnsFalseForMatches() {
        assertFalse(unmarshaller.matches(500));
    }

    @Test
    public void jsonObjectWithData_UnmarshallsIntoModeledException() throws Exception {
        final BaseException exception = unmarshaller
                .unmarshall(new ObjectMapper().readTree("{\"foo\": \"value\"}"));
        assertThat(exception, instanceOf(ModeledException.class));
        assertEquals("value", ((ModeledException) exception).getFoo());
    }

    @Test
    public void emptyJsonObject_UnmarshallsIntoModeledException() throws Exception {
        final BaseException exception = unmarshaller
                .unmarshall(new ObjectMapper().readTree("{}"));
        assertThat(exception, instanceOf(ModeledException.class));
        assertEquals(null, ((ModeledException) exception).getFoo());
    }

    @Test
    public void nullHttpStatusCodeMatchesAllExceptions() {
        ApiGatewayErrorUnmarshaller allMatchUnmarshaller = new ApiGatewayErrorUnmarshaller(
                ModeledException.class, Optional.empty());
        assertTrue(allMatchUnmarshaller.matches(400));
        assertTrue(allMatchUnmarshaller.matches(500));
    }

    private static class ModeledException extends SdkBaseException implements BaseException {

        private String message;
        private SdkErrorHttpMetadata sdkHttpMetadata;

        private String foo;

        public ModeledException(String message) {
            super(message);
        }

        @JsonProperty("foo")
        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        @Override
        public ModeledException sdkHttpMetadata(SdkErrorHttpMetadata sdkHttpMetadata) {
            this.sdkHttpMetadata = sdkHttpMetadata;
            return this;
        }

        @Override
        public SdkErrorHttpMetadata sdkHttpMetadata() {
            return sdkHttpMetadata;
        }

        @Override
        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
