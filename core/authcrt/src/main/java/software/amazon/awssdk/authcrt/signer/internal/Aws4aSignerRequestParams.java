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

package software.amazon.awssdk.authcrt.signer.internal;

import java.time.Clock;
import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.authcrt.signer.params.Aws4aSignerParams;

/**
 * Parameters that are used for computing an AWS 4a signature for a request.
 */
@SdkInternalApi
public final class Aws4aSignerRequestParams {

    private final Clock signingClock;

    /**
     * Generates an instance of AWS4asignerRequestParams that holds the
     * parameters used for computing a AWS 4 signature for a request based on
     * the given {@link Aws4SignerParams} for that request.
     */
    public Aws4aSignerRequestParams(Aws4aSignerParams signerParams) {
        this.signingClock = resolveSigningClock(signerParams);
    }

    /**
     * @return The clock to use for signing additional data i.e. events or chunks.
     */
    public Clock getSigningClock() {
        return signingClock;
    }

    private Clock resolveSigningClock(Aws4aSignerParams signerParams) {
        if (signerParams.signingClockOverride().isPresent()) {
            return signerParams.signingClockOverride().get();
        }
        Clock baseClock = Clock.systemUTC();
        return signerParams.timeOffset()
                .map(offset -> Clock.offset(baseClock, Duration.ofSeconds(-offset)))
                .orElse(baseClock);
    }
}
