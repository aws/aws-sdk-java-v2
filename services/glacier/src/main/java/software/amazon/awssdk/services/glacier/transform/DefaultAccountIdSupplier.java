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

package software.amazon.awssdk.services.glacier.transform;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;

@ReviewBeforeRelease("If we refactor our marshallers, this should also be refactored to match.")
public final class DefaultAccountIdSupplier {

    /**
     * Value that indicates the current account.
     */
    private static final String CURRENT_ACCOUNT_ID = "-";

    private static final Supplier<String> INSTANCE = () -> CURRENT_ACCOUNT_ID;

    private DefaultAccountIdSupplier() {
    }

    public static Supplier<String> getInstance() {
        return INSTANCE;
    }
}
