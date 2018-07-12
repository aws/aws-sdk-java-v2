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

package software.amazon.awssdk.awscore.retry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import java.util.function.Consumer;
import org.junit.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.retry.conditions.RetryOnErrorCodeCondition;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.RetryPolicyContext;

public class RetryOnErrorCodeConditionTest {

    private RetryOnErrorCodeCondition condition = RetryOnErrorCodeCondition.create(Sets.newHashSet("Foo", "Bar"));

    @Test
    public void noExceptionInContext_ReturnsFalse() {
        assertFalse(shouldRetry(builder -> builder.exception(null)));
    }

    @Test
    public void retryableErrorCodes_ReturnsTrue() {
        assertTrue(shouldRetry(applyErrorCode("Foo")));
        assertTrue(shouldRetry(applyErrorCode("Bar")));
    }

    @Test
    public void nonRetryableErrorCode_ReturnsFalse() {
        assertFalse(shouldRetry(applyErrorCode("HelloWorld")));
    }

    private boolean shouldRetry(Consumer<RetryPolicyContext.Builder> builder) {
        return condition.shouldRetry(RetryPolicyContext.builder().applyMutation(builder).build());
    }

    private Consumer<RetryPolicyContext.Builder> applyErrorCode(String errorCode) {
        AwsServiceException.Builder exception = AwsServiceException.builder();
        exception.statusCode(404);
        exception.awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).build());
        return b -> b.exception(exception.build());
    }
}
