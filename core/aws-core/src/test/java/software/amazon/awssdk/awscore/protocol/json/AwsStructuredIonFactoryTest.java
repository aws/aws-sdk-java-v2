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

package software.amazon.awssdk.awscore.protocol.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.awscore.client.utils.ValidSdkObjects;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.http.response.AwsJsonErrorResponseHandler;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsJsonErrorUnmarshaller;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsStructuredIonFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.ion.IonStruct;
import software.amazon.ion.IonSystem;
import software.amazon.ion.IonWriter;
import software.amazon.ion.Timestamp;
import software.amazon.ion.system.IonSystemBuilder;

public class AwsStructuredIonFactoryTest {
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
        payload.add("errorMessage", system.newString(ERROR_MESSAGE));
        return payload;
    }

    private static HttpResponse createResponse(IonStruct payload) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        IonWriter writer = system.newBinaryWriter(bytes);
        payload.writeTo(writer);
        writer.close();

        HttpResponse error = new HttpResponse(ValidSdkObjects.sdkHttpFullRequest().build());
        error.setContent(new ByteArrayInputStream(bytes.toByteArray()));
        return error;
    }

    @Test
    public void handlesErrorsUsingHttpHeader() throws Exception {
        IonStruct payload = createPayload();

        HttpResponse error = createResponse(payload);
        error.addHeader("x-amzn-ErrorType", ERROR_TYPE);

        AwsServiceException exception = handleError(error);
        assertThat(exception).isInstanceOf(InvalidParameterException.class);
        assertEquals(ERROR_MESSAGE, exception.awsErrorDetails().errorMessage());
    }

    @Test
    public void handlesErrorsUsingMagicField() throws Exception {
        IonStruct payload = createPayload();
        payload.add("__type", system.newString(ERROR_TYPE));

        HttpResponse error = createResponse(payload);

        AwsServiceException exception = handleError(error);
        assertThat(exception).isInstanceOf(InvalidParameterException.class);
        assertEquals(ERROR_MESSAGE, exception.awsErrorDetails().errorMessage());
    }

    @Test
    public void handlesErrorsUsingAnnotation() throws Exception {
        IonStruct payload = createPayload();
        payload.addTypeAnnotation(ERROR_PREFIX + ERROR_TYPE);

        HttpResponse error = createResponse(payload);

        AwsServiceException exception = handleError(error);
        assertThat(exception).isInstanceOf(InvalidParameterException.class);
        assertEquals(ERROR_MESSAGE, exception.awsErrorDetails().errorMessage());
    }

    @Test(expected = SdkClientException.class)
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

        AwsServiceException exception = handleError(error);
        assertThat(exception).isInstanceOf(InvalidParameterException.class);
        assertEquals(ERROR_MESSAGE, exception.awsErrorDetails().errorMessage());
    }

    private AwsServiceException handleError(HttpResponse error) throws Exception {
        List<AwsJsonErrorUnmarshaller> unmarshallers = new LinkedList<>();
        unmarshallers.add(new AwsJsonErrorUnmarshaller(InvalidParameterException.class, ERROR_TYPE));

        AwsJsonErrorResponseHandler handler = AwsStructuredIonFactory.SDK_ION_BINARY_FACTORY
                .createErrorResponseHandler(unmarshallers, NO_CUSTOM_ERROR_CODE_FIELD_NAME);
        return handler.handle(error, new ExecutionAttributes());
    }

    private static class InvalidParameterException extends AwsServiceException {
        private static final long serialVersionUID = 0;

        public InvalidParameterException(BeanStyleBuilder builder) {
            super(builder);
        }

        public static Class<? extends Builder> serializableBuilderClass() {
            return BeanStyleBuilder.class;
        }

        @Override
        public Builder toBuilder() {
            return new BeanStyleBuilder(this);
        }

        public interface Builder extends AwsServiceException.Builder {
            @Override
            Builder message(String message);

            @Override
            InvalidParameterException build();
        }

        private static class BeanStyleBuilder extends AwsServiceException.BuilderImpl implements Builder {
            private String message;

            private BeanStyleBuilder() {}

            private BeanStyleBuilder(InvalidParameterException ex) {
                this.message = ex.getMessage();
            }

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
