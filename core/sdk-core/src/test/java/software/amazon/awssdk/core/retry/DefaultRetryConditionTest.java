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
package software.amazon.awssdk.core.retry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.function.Consumer;
import org.junit.Test;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;

public class DefaultRetryConditionTest {

    @Test
    public void retriesOnThrottlingExceptions() {
        assertTrue(shouldRetry(applyStatusCode(429)));
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
        assertTrue(shouldRetry(b -> b.exception(SdkClientException.builder().message("IO").cause(new IOException()).build())));
    }

    @Test
    public void retriesOnRetryableException() {
        assertTrue(shouldRetry(b -> b.exception(RetryableException.builder().message("this is retryable").build())));
    }

    @Test
    public void doesNotRetryOnNonRetryableException() {
        assertFalse(shouldRetry(b -> b.exception(NonRetryableException.builder().message("this is NOT retryable").build())));
    }

    @Test
    public void doesNotRetryOnNonRetryableStatusCode() {
        assertFalse(shouldRetry(applyStatusCode(404)));
    }

    private boolean shouldRetry(Consumer<RetryPolicyContext.Builder> builder) {
        return RetryCondition.defaultRetryCondition().shouldRetry(RetryPolicyContext.builder()
                                                                    .applyMutation(builder)
                                                                    .build());
    }

    private Consumer<RetryPolicyContext.Builder> applyStatusCode(Integer statusCode) {
        SdkServiceException exception = SdkServiceException.builder().statusCode(statusCode).build();
        return b -> b.exception(exception)
                     .httpStatusCode(statusCode);
    }
}
