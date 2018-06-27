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

package software.amazon.awssdk.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.RetryUtils;
import software.amazon.awssdk.http.HttpStatusCode;

public class RetryUtilsTest {

    @Test
    public void nonSdkServiceException_shouldReturnFalse() {
        SdkClientException exception = new SdkClientException("exception");
        assertThat(RetryUtils.isServiceException(exception)).isFalse();
        assertThat(RetryUtils.isClockSkewException(exception)).isFalse();
        assertThat(RetryUtils.isThrottlingException(exception)).isFalse();
        assertThat(RetryUtils.isRequestEntityTooLargeException(exception)).isFalse();
    }

    @Test
    public void statusCode429_isThrottlingExceptionShouldReturnTrue() {
        SdkServiceException throttlingException = new SdkServiceException("throttling");
        throttlingException.statusCode(429);
        assertThat(RetryUtils.isThrottlingException(throttlingException)).isTrue();
    }

    @Test
    public void sdkServiceException_shouldReturnFalseIfNotOverridden() {
        SdkServiceException clockSkewException = new SdkServiceException("default");
        assertThat(RetryUtils.isClockSkewException(clockSkewException)).isFalse();
    }

    @Test
    public void clockSkewException_shouldReturnTrue() {
        SdkServiceException clockSkewException = new SdkServiceException("clockSkew") {
            @Override
            public boolean isClockSkewException() {
                return true;
            }
        };

        assertThat(RetryUtils.isClockSkewException(clockSkewException)).isTrue();
    }

    @Test
    public void statusCode413_isRequestEntityTooLargeShouldReturnTrue() {
        SdkServiceException exception = new SdkServiceException("boom");
        exception.statusCode(HttpStatusCode.REQUEST_TOO_LONG);
        assertThat(RetryUtils.isRequestEntityTooLargeException(exception)).isTrue();
    }
}
