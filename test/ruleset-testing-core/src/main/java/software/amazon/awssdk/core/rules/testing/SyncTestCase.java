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

package software.amazon.awssdk.core.rules.testing;

import software.amazon.awssdk.core.rules.testing.model.Expect;

public final class SyncTestCase {
    private final String description;
    private final Runnable operationRunnable;
    private final Expect expectation;
    private final String skipReason;

    public SyncTestCase(String description, Runnable operationRunnable, Expect expectation) {
        this(description, operationRunnable, expectation, null);
    }

    public SyncTestCase(String description, Runnable operationRunnable, Expect expectation, String skipReason) {
        this.description = description;
        this.operationRunnable = operationRunnable;
        this.expectation = expectation;
        this.skipReason = skipReason;
    }

    public Runnable operationRunnable() {
        return operationRunnable;
    }

    public Expect expectation() {
        return expectation;
    }

    public String skipReason() {
        return skipReason;
    }

    @Override
    public String toString() {
        return description;
    }
}
