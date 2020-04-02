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
package software.amazon.awssdk.awscore.retry;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.awscore.retry.AwsRetryPolicy.defaultRetryCondition;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import org.junit.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.DateUtils;

public class AwsRetryPolicyTest {

    @Test
    public void retriesOnRetryableErrorCodes() {
        assertTrue(shouldRetry(applyErrorCode("PriorRequestNotComplete")));
    }

    @Test
    public void retriesOnThrottlingExceptions() {
        assertTrue(shouldRetry(applyErrorCode("ThrottlingException")));
        assertTrue(shouldRetry(applyErrorCode("ThrottledException")));
        assertTrue(shouldRetry(applyStatusCode(429)));
    }

    @Test
    public void retriesOnClockSkewErrors() {
        assertTrue(shouldRetry(applyErrorCode("RequestTimeTooSkewed")));
        assertTrue(shouldRetry(applyErrorCode("AuthFailure", Duration.ZERO, Instant.now().minus(1, HOURS))));
    }

    @Test
    public void retriesOnInternalError() {
        assertTrue(shouldRetry(applyStatusCode(500)));
    }

    @Test
    public void retriesOnBadGateway() {
        assertTrue(shouldRetry(applyStatusCode(502)));
    }

    @Test
    public void retriesOnServiceUnavailable() {
        assertTrue(shouldRetry(applyStatusCode(503)));
    }

    @Test
    public void retriesOnGatewayTimeout() {
        assertTrue(shouldRetry(applyStatusCode(504)));
    }

    @Test
    public void retriesOnIOException() {
        assertTrue(shouldRetry(b -> b.exception(SdkClientException.builder()
                                                                  .message("IO")
                                                                  .cause(new IOException())
                                                                  .build())));
    }

    @Test
    public void retriesOnRetryableException() {
        assertTrue(shouldRetry(b -> b.exception(RetryableException.builder().build())));
    }

    @Test
    public void doesNotRetryOnNonRetryableException() {
        assertFalse(shouldRetry(b -> b.exception(NonRetryableException.builder().build())));
    }

    @Test
    public void doesNotRetryOnNonRetryableStatusCode() {
        assertFalse(shouldRetry(applyStatusCode(404)));
    }

    @Test
    public void doesNotRetryOnNonRetryableErrorCode() {
        assertFalse(shouldRetry(applyErrorCode("ValidationError")));
    }

    @Test
    public void retriesOnEC2ThrottledException() {
        AwsServiceException ex = AwsServiceException.builder()
                                                    .awsErrorDetails(AwsErrorDetails.builder()
                                                                                    .errorCode("EC2ThrottledException")
                                                                                    .build())
                                                    .build();

        assertTrue(shouldRetry(b -> b.exception(ex)));
    }

    private boolean shouldRetry(Consumer<RetryPolicyContext.Builder> builder) {
        return defaultRetryCondition().shouldRetry(RetryPolicyContext.builder().applyMutation(builder).build());
    }

    private Consumer<RetryPolicyContext.Builder> applyErrorCode(String errorCode) {
        AwsServiceException.Builder exception = AwsServiceException.builder().statusCode(404);
        exception.awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).build());
        return b -> b.exception(exception.build());
    }


    private Consumer<RetryPolicyContext.Builder> applyErrorCode(String errorCode, Duration clockSkew, Instant dateHeader) {
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                                                          .putHeader("Date", DateUtils.formatRfc1123Date(dateHeader))
                                                          .build();

        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                                                      .errorCode(errorCode)
                                                      .sdkHttpResponse(response)
                                                      .build();

        AwsServiceException.Builder exception = AwsServiceException.builder()
                                                                   .statusCode(404)
                                                                   .awsErrorDetails(errorDetails)
                                                                   .clockSkew(clockSkew);
        return b -> b.exception(exception.build());
    }

    private Consumer<RetryPolicyContext.Builder> applyStatusCode(Integer statusCode) {
        AwsServiceException.Builder exception = AwsServiceException.builder().statusCode(statusCode);
        exception.awsErrorDetails(AwsErrorDetails.builder().errorCode("Foo").build());
        return b -> b.exception(exception.build())
                     .httpStatusCode(statusCode);
    }
}
