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
package software.amazon.awssdk.services.s3control.internal.interceptors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3control.model.InvalidRequestException;
import software.amazon.awssdk.services.s3control.model.S3ControlException;

public class TopLevelXMLErrorInterceptorTest {

    @Test
    public void when_correctlyParsedException_returnsExceptionUnmodified() {
        AwsServiceException originalException = S3ControlException.builder()
                                                                  .message("This is a correctly parsed error")
                                                                  .build();
        TopLevelXMLErrorInterceptor interceptor = new TopLevelXMLErrorInterceptor();
        Throwable translatedException = interceptor.modifyException(new Context(originalException), new ExecutionAttributes());
        assertThat(translatedException).isEqualTo(originalException);
    }

    @Test
    public void when_incorrectlyParsedException_wrongXMLStructure_returnsExceptionUnmodified() {
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<UnrecognizedRoot>\n"
                                 + "<Tag>Value</Tag>\n"
                                 + "</UnrecognizedRoot>";
        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder()
                                                         .rawResponse(SdkBytes.fromUtf8String(xmlResponseBody))
                                                         .build();

        AwsServiceException originalException = S3ControlException.builder()
                                                                  .message("Error message with null")
                                                                  .awsErrorDetails(awsErrorDetails)
                                                                  .build();
        TopLevelXMLErrorInterceptor interceptor = new TopLevelXMLErrorInterceptor();
        Throwable translatedException = interceptor.modifyException(new Context(originalException), new ExecutionAttributes());
        assertThat(translatedException).isEqualTo(originalException);
    }

    @Test
    public void when_incorrectlyParsedException_correctXMLStructure_returnsSpecificException() {
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<Error>\n"
                                 + "<Code>InvalidRequest</Code>\n"
                                 + "<Message>Missing role arn</Message>\n"
                                 + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                                 + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                                 + "</Error>";
        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder()
                                                         .rawResponse(SdkBytes.fromUtf8String(xmlResponseBody))
                                                         .build();

        AwsServiceException originalException = S3ControlException.builder()
                                                                  .message("Error message with null")
                                                                  .awsErrorDetails(awsErrorDetails)
                                                                  .build();
        TopLevelXMLErrorInterceptor interceptor = new TopLevelXMLErrorInterceptor();
        Throwable translatedException = interceptor.modifyException(new Context(originalException), new ExecutionAttributes());
        assertThat(translatedException).isNotEqualTo(originalException);
        assertThat(translatedException).isInstanceOf(InvalidRequestException.class);
        InvalidRequestException s3ControlException = (InvalidRequestException) translatedException;
        assertThat(s3ControlException.awsErrorDetails().errorCode()).isEqualTo("InvalidRequest");
        assertThat(s3ControlException.awsErrorDetails().errorMessage()).isEqualTo("Missing role arn");
    }

    @Test
    public void when_incorrectlyParsedException_correctXMLStructure_returnsGenericException() {
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<Error>\n"
                                 + "<Code>SomeOtherException</Code>\n"
                                 + "<Message>The exception message</Message>\n"
                                 + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                                 + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                                 + "</Error>";
        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder()
                                                         .rawResponse(SdkBytes.fromUtf8String(xmlResponseBody))
                                                         .build();

        AwsServiceException originalException = S3ControlException.builder()
                                                                  .message("Error message with null")
                                                                  .awsErrorDetails(awsErrorDetails)
                                                                  .build();
        TopLevelXMLErrorInterceptor interceptor = new TopLevelXMLErrorInterceptor();
        Throwable translatedException = interceptor.modifyException(new Context(originalException), new ExecutionAttributes());
        assertThat(translatedException).isNotEqualTo(originalException);
        assertThat(translatedException).isInstanceOf(S3ControlException.class);
        S3ControlException s3ControlException = (S3ControlException) translatedException;
        assertThat(s3ControlException.awsErrorDetails().errorCode()).isEqualTo("SomeOtherException");
        assertThat(s3ControlException.awsErrorDetails().errorMessage()).isEqualTo("The exception message");
    }

    public static final class Context implements software.amazon.awssdk.core.interceptor.Context.FailedExecution {

        private final Throwable exception;

        public Context(Throwable exception) {
            this.exception = exception;
        }

        @Override
        public Throwable exception() { return exception; }

        @Override
        public SdkRequest request() {
            return null;
        }

        @Override
        public Optional<SdkResponse> response() { return Optional.empty(); }

        @Override
        public Optional<SdkHttpRequest> httpRequest() {
            return Optional.empty();
        }

        @Override
        public Optional<SdkHttpResponse> httpResponse() {
            return Optional.empty();
        }

    }
}
