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

package software.amazon.awssdk.services.dynamodb;

import org.junit.Test;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.conditions.OrRetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.testutils.service.AwsTestBase;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Simple test to check all the retries are made when all the API calls timeouts.
 */
public class DynamoDbJavaClientRetryOnTimeoutIntegrationTest extends AwsTestBase {

    private static DynamoDbClient ddb;
    private final Integer RETRY_ATTEMPTS = 3;

    public static void setupWithRetryPolicy(RetryPolicy retryPolicy, Integer attemptTimeOutMillis) throws Exception {
        ddb = DynamoDbClient.builder()
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .retryPolicy(retryPolicy)
                        .apiCallAttemptTimeout(Duration.ofMillis(attemptTimeOutMillis)) //forces really quick api call timeout
                        .build())
                .build();

    }

    public static RetryPolicy createRetryPolicyWithCounter(AtomicInteger counter, Integer numOfAttempts) {
        final RetryPolicy retryPolicy = RetryPolicy.defaultRetryPolicy().toBuilder()
                .numRetries(numOfAttempts)
                .retryCondition(OrRetryCondition.create(
                        context -> {
                            counter.incrementAndGet();
                            return false;
                        }, RetryCondition.defaultRetryCondition())).build();

        return retryPolicy;

    }

    @Test
    public void testRetryAttemptsOnTimeOut() throws Exception {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        Boolean apiTimeOutExceptionOccurred = Boolean.FALSE;
        setupWithRetryPolicy(createRetryPolicyWithCounter(atomicInteger, RETRY_ATTEMPTS), 2);
        try {

            ddb.listTables();
        } catch (ApiCallAttemptTimeoutException e) {
            apiTimeOutExceptionOccurred = true;
        }
        assertEquals(RETRY_ATTEMPTS.intValue(), atomicInteger.get());
        assertTrue(apiTimeOutExceptionOccurred);
    }

}
