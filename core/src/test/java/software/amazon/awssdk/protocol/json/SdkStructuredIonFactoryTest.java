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

package software.amazon.awssdk.protocol.json;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.internal.http.response.JsonErrorResponseHandler;
import software.amazon.awssdk.runtime.transform.JsonErrorUnmarshaller;
import software.amazon.ion.IonStruct;
import software.amazon.ion.IonSystem;
import software.amazon.ion.IonWriter;
import software.amazon.ion.Timestamp;
import software.amazon.ion.system.IonSystemBuilder;

public class SdkStructuredIonFactoryTest {
    private static final String ERROR_PREFIX = "aws-type:";
    private static final String ERROR_TYPE = "InvalidParameterException";
    private static final String ERROR_MESSAGE = "foo";

    private static final String NO_CUSTOM_ERROR_CODE_FIELD_NAME = null;

    private static IonSystem system;

    @BeforeClass
    public static void beforeClass() {
        system = IonSystemBuilder.standard().build();
    }

    private static IonStruct createPayload() {
        IonStruct payload = system.newEmptyStruct();
        payload.add("NotValidJson", system.newTimestamp(Timestamp.nowZ()));
        payload.add("ErrorMessage", system.newString(ERROR_MESSAGE));
        return payload;
    }

    private static HttpResponse createResponse(IonStruct payload) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        IonWriter writer = system.newBinaryWriter(bytes);
        payload.writeTo(writer);
        writer.close();

        HttpResponse error = new HttpResponse(SdkHttpFullRequest.builder().build());
        error.setContent(new ByteArrayInputStream(bytes.toByteArray()));
        return error;
    }

    @Test
    public void handlesErrorsUsingHttpHeader() throws Exception {
        IonStruct payload = createPayload();

        HttpResponse error = createResponse(payload);
        error.addHeader("x-amzn-ErrorType", ERROR_TYPE);

        AmazonServiceException exception = handleError(error);
        assertThat(exception, instanceOf(InvalidParameterException.class));
        assertEquals(ERROR_MESSAGE, exception.getErrorMessage());
    }

    @Test
    public void handlesErrorsUsingMagicField() throws Exception {
        IonStruct payload = createPayload();
        payload.add("__type", system.newString(ERROR_TYPE));

        HttpResponse error = createResponse(payload);

        AmazonServiceException exception = handleError(error);
        assertThat(exception, instanceOf(InvalidParameterException.class));
        assertEquals(ERROR_MESSAGE, exception.getErrorMessage());
    }

    @Test
    public void handlesErrorsUsingAnnotation() throws Exception {
        IonStruct payload = createPayload();
        payload.addTypeAnnotation(ERROR_PREFIX + ERROR_TYPE);

        HttpResponse error = createResponse(payload);

        AmazonServiceException exception = handleError(error);
        assertThat(exception, instanceOf(InvalidParameterException.class));
        assertEquals(ERROR_MESSAGE, exception.getErrorMessage());
    }

    @Test(expected = AmazonClientException.class)
    public void rejectPayloadsWithMultipleErrorAnnotations() throws Exception {
        IonStruct payload = createPayload();
        payload.addTypeAnnotation(ERROR_PREFIX + ERROR_TYPE);
        payload.addTypeAnnotation(ERROR_PREFIX + "foo");

        HttpResponse error = createResponse(payload);

        handleError(error);
    }

    @Test
    public void handlesErrorsWithMutipleAnnotations() throws Exception {
        IonStruct payload = createPayload();
        payload.addTypeAnnotation("foo");
        payload.addTypeAnnotation(ERROR_PREFIX + ERROR_TYPE);
        payload.addTypeAnnotation("bar");

        HttpResponse error = createResponse(payload);

        AmazonServiceException exception = handleError(error);
        assertThat(exception, instanceOf(InvalidParameterException.class));
        assertEquals(ERROR_MESSAGE, exception.getErrorMessage());
    }

    private AmazonServiceException handleError(HttpResponse error) throws Exception {
        List<JsonErrorUnmarshaller> unmarshallers = new LinkedList<>();
        unmarshallers.add(new JsonErrorUnmarshaller(InvalidParameterException.class, ERROR_TYPE));

        JsonErrorResponseHandler handler = SdkStructuredIonFactory.SDK_ION_BINARY_FACTORY
                .createErrorResponseHandler(unmarshallers, NO_CUSTOM_ERROR_CODE_FIELD_NAME);
        return handler.handle(error, new ExecutionAttributes());
    }

    private static class InvalidParameterException extends AmazonServiceException {
        private static final long serialVersionUID = 0;

        public InvalidParameterException(BeanStyleBuilder builder) {
            super(builder.message);
        }

        public static Class<?> serializableBuilderClass() {
            return BeanStyleBuilder.class;
        }

        public interface Builder {
            Builder message(String message);
            InvalidParameterException build();
        }

        private static class BeanStyleBuilder implements Builder {
            private String message;

            @Override
            public Builder message(String message) {
                this.message = message;
                return this;
            }

            @JsonProperty("ErrorMessage")
            public void setMessage(String message) {
                this.message = message;
            }

            @Override
            public InvalidParameterException build() {
                return new InvalidParameterException(this);
            }
        }
    }
}
