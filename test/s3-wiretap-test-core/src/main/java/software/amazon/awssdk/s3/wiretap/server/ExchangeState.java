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

package software.amazon.awssdk.s3.wiretap.server;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public enum ExchangeState {

    REQUEST_STREAMING(0),

    AWAITING_RESPONSE(1),

    RESPONSE_STREAMING(2),

    COMPLETE(3),

    RESPONSE_TRUNCATED(3),

    REQUEST_ABORTED(3),

    NO_RESPONSE(3),

    FAILED(3);

    private static final int TERMINAL = 3;

    @Getter(AccessLevel.PACKAGE)
    private final int rank;

    ExchangeState(int rank) {
        this.rank = rank;
    }

    public boolean terminal() {
        return rank == TERMINAL;
    }
}
