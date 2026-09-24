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

package software.amazon.awssdk.awscore.presigner;

import java.time.Duration;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Utilities for computing the effective expiration of a presigned request.
 */
@SdkProtectedApi
public final class PresignExpirationUtils {

    private PresignExpirationUtils() {
    }

    /**
     * Returns the requested presign duration, capped to the whole seconds remaining before the signing credential expires,
     * because a presigned request stops working once its credentials expire.
     *
     * <p>The requested duration is returned unchanged if the credential has no expiration, or if less than one second of its
     * lifetime remains. Credentials may report an expiration in the past and still be valid (static stability), and the
     * SigV4 signer rejects durations under one second.
     *
     * @param requestedDuration the duration the caller requested.
     * @param signingInstant the instant the request is signed at.
     * @param credentialExpiration the signing credential's expiration, or null if it has none.
     * @return the duration to sign the presigned request for.
     */
    public static Duration effectiveExpirationDuration(Duration requestedDuration,
                                                       Instant signingInstant,
                                                       Instant credentialExpiration) {
        if (credentialExpiration == null) {
            return requestedDuration;
        }
        long secondsUntilExpiration = Duration.between(signingInstant, credentialExpiration).getSeconds();
        if (secondsUntilExpiration <= 0) {
            return requestedDuration;
        }
        Duration untilExpiration = Duration.ofSeconds(secondsUntilExpiration);
        return untilExpiration.compareTo(requestedDuration) < 0 ? untilExpiration : requestedDuration;
    }
}
