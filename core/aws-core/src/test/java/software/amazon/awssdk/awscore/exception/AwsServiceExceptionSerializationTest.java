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

package software.amazon.awssdk.awscore.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.StringInputStream;

public class AwsServiceExceptionSerializationTest {

    @Test
    public void serializeBasicServiceException() throws Exception {
        AwsServiceException expectedException = createException();
        AwsServiceException resultException = serializeServiceException(expectedException);
        assertSameValues(resultException, expectedException);
    }

    @Test
    public void serializeRetryableServiceException() throws Exception {
        AwsServiceException expectedException = createRetryableServiceException();
        AwsServiceException resultException = serializeServiceException(expectedException);
        assertSameValues(resultException, expectedException);
    }

    private void assertSameValues(AwsServiceException result, AwsServiceException expected) {
        assertThat(result.getMessage()).isEqualTo(expected.getMessage());
        assertThat(result.requestId()).isEqualTo(expected.requestId());
        assertThat(result.extendedRequestId()).isEqualTo(expected.extendedRequestId());
        assertThat(result.toBuilder().clockSkew()).isEqualTo(expected.toBuilder().clockSkew());
        assertThat(result.toBuilder().cause().getMessage()).isEqualTo(expected.toBuilder().cause().getMessage());
        assertThat(result.awsErrorDetails()).isEqualTo(expected.awsErrorDetails());
        assertThat(result.numAttempts()).isEqualTo(expected.numAttempts());
    }

    private AwsServiceException createException() {
        return AwsServiceException.builder()
                                  .awsErrorDetails(createErrorDetails(403, "SomeText"))
                                  .statusCode(403)
                                  .cause(new RuntimeException("someThrowable"))
                                  .clockSkew(Duration.ofSeconds(2))
                                  .requestId("requestId")
                                  .extendedRequestId("extendedRequestId")
                                  .message("message")
                                  .build();
    }

    private AwsServiceException createRetryableServiceException() {
        return AwsServiceException.builder()
                                  .awsErrorDetails(createErrorDetails(429, "Throttling"))
                                  .statusCode(429)
                                  .cause(new RuntimeException("someThrowable"))
                                  .clockSkew(Duration.ofSeconds(2))
                                  .requestId("requestId")
                                  .extendedRequestId("extendedRequestId")
                                  .message("message")
                                  .numAttempts(3)
                                  .build();
    }

    private AwsErrorDetails createErrorDetails(int statusCode, String statusText) {
        AbortableInputStream contentStream = AbortableInputStream.create(new StringInputStream("some content"));
        SdkHttpResponse httpResponse = SdkHttpFullResponse.builder()
                                                          .statusCode(statusCode)
                                                          .statusText(statusText)
                                                          .content(contentStream)
                                                          .build();

        return AwsErrorDetails.builder()
                              .errorCode("someCode")
                              .errorMessage("message")
                              .serviceName("someService")
                              .sdkHttpResponse(httpResponse)
                              .build();
    }

    private AwsServiceException serializeServiceException(AwsServiceException exception) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(exception);
        objectOutputStream.flush();
        objectOutputStream.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
        return (AwsServiceException) ois.readObject();
    }
}