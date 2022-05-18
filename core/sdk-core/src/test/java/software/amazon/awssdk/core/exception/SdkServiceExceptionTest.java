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

package software.amazon.awssdk.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class SdkServiceExceptionTest {

    @Test
    public void requestIdIsPopulated() {
        SdkServiceException actual = SdkServiceException.builder().requestId("req").build();
        assertThat(actual.requestId()).isEqualTo("req");
    }

    @Test
    public void extendedRequestIdIsPopulated() {
        SdkServiceException actual = SdkServiceException.builder().extendedRequestId("req").build();
        assertThat(actual.extendedRequestId()).isEqualTo("req");
    }

    @Test
    public void statusCodeIsPopulated() {
        SdkServiceException actual = exception(123);
        assertThat(actual.statusCode()).isEqualTo(123);
    }

    @Test
    public void isClockSkewExceptionIsFalse() {
        SdkServiceException actual = SdkServiceException.builder().build();
        assertThat(actual.isClockSkewException()).isFalse();
    }

    @Test
    public void throttlingErrorsAreIdentifiedCorrectly() {
        assertThrottling(429);
    }

    @Test
    public void nonThrottlingErrorsAreIdentifiedCorrectly() {
        assertNotThrottling(400);
        assertNotThrottling(500);
    }

    @Test
    public void retryableErrorsAreIdentifiedCorrectly() {
        assertRetryable(429);
        assertRetryable(500);
        assertRetryable(502);
        assertRetryable(503);
        assertRetryable(504);
    }

    @Test
    public void nonRetryableErrorsAreIdentifiedCorrectly() {
        assertNotRetryable(400);
        assertNotRetryable(501);
        assertNotRetryable(505);
    }

    private static void assertThrottling(int statusCode) {
        SdkServiceException exception = exception(statusCode);

        assertThat(exception.isThrottlingException()).isTrue();
    }

    private static void assertNotThrottling(int statusCode) {
        SdkServiceException exception = exception(statusCode);

        assertThat(exception.isThrottlingException()).isFalse();
    }

    private static void assertRetryable(int statusCode) {
        SdkServiceException exception = exception(statusCode);

        assertThat(exception.retryable()).isTrue();

    }

    private static void assertNotRetryable(int statusCode) {
        SdkServiceException exception = exception(statusCode);

        assertThat(exception.retryable()).isFalse();

    }

    private static SdkServiceException exception(int statusCode) {
        return SdkServiceException.builder().statusCode(statusCode).build();
    }
}
