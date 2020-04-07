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

package software.amazon.awssdk.auth.signer.internal;

import java.time.Instant;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.DateUtils;

/**
 * Holds the signing key and the number of days since epoch for the date for
 * which the signing key was generated.
 */
@Immutable
@SdkInternalApi
public final class SignerKey {

    private final long daysSinceEpoch;

    private final byte[] signingKey;

    public SignerKey(Instant date, byte[] signingKey) {
        if (date == null) {
            throw new IllegalArgumentException(
                    "Not able to cache signing key. Signing date to be is null");
        }
        if (signingKey == null) {
            throw new IllegalArgumentException(
                    "Not able to cache signing key. Signing Key to be cached are null");
        }
        this.daysSinceEpoch = DateUtils.numberOfDaysSinceEpoch(date.toEpochMilli());
        this.signingKey = signingKey.clone();
    }

    public boolean isValidForDate(Instant other) {
        return daysSinceEpoch == DateUtils.numberOfDaysSinceEpoch(other.toEpochMilli());
    }

    /**
     * Returns a copy of the signing key.
     */
    public byte[] getSigningKey() {
        return signingKey.clone();
    }
}
