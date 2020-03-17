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

package software.amazon.awssdk.testutils.retry;

/**
 * Utility to repeatedly invoke assertion logic until it succeeds or the max allowed attempts is
 * reached.
 */
public final class RetryableAssertion {

    private RetryableAssertion() {
    }

    /**
     * Static method to repeatedly call assertion logic until it succeeds or the max allowed
     * attempts is reached.
     *
     * @param callable Callable implementing assertion logic
     * @param params   Retry related parameters
     */
    public static void doRetryableAssert(AssertCallable callable, RetryableParams params) throws
                                                                                          Exception {
        RetryableAction.doRetryableAction(callable, params);
    }
}
