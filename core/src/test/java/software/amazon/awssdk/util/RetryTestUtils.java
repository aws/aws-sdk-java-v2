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

package software.amazon.awssdk.util;

import org.junit.Assert;
import software.amazon.awssdk.http.ExecutionContext;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;

public class RetryTestUtils {

    /**
     * Checks the RequestCount metric and compares it against the expected value.
     * @param expectedRetryAttempts number of expected retries
     * @param context request execution context
     */
    public static void assertExpectedRetryCount(final int expectedRetryAttempts, final ExecutionContext context) {
        Assert.assertEquals(
                expectedRetryAttempts + 1, // request count = retries + 1
                context.awsRequestMetrics()
                       .getTimingInfo().getCounter(AwsRequestMetrics.Field.RequestCount.toString()).intValue());
    }

}
