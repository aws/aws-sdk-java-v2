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

import java.time.Clock;
import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.regions.Region;

/**
 * Parameters that are used for computing a AWS 4 signature for a request.
 */
@SdkInternalApi
public final class Aws4SignerRequestParams {

    private final Clock signingClock;

    private final long requestSigningDateTimeMilli;

    /**
     * The scope of the signature.
     */
    private final String scope;

    /**
     * The AWS region to be used for computing the signature.
     */
    private final String regionName;

    /**
     * The name of the AWS service.
     */
    private final String serviceSigningName;

    /**
     * UTC formatted version of the signing time stamp.
     */
    private final String formattedRequestSigningDateTime;

    /**
     * UTC Formatted Signing date with time stamp stripped.
     */
    private final String formattedRequestSigningDate;

    /**
     * Generates an instance of AWS4signerRequestParams that holds the
     * parameters used for computing a AWS 4 signature for a request based on
     * the given {@link Aws4SignerParams} for that request.
     */
    public Aws4SignerRequestParams(Aws4SignerParams signerParams) {
        this.signingClock = resolveSigningClock(signerParams);
        this.requestSigningDateTimeMilli = this.signingClock.millis();
        this.formattedRequestSigningDate = Aws4SignerUtils.formatDateStamp(requestSigningDateTimeMilli);
        this.serviceSigningName = signerParams.signingName();
        this.regionName = getRegion(signerParams.signingRegion());
        this.scope = generateScope(formattedRequestSigningDate, this.serviceSigningName, regionName);
        this.formattedRequestSigningDateTime = Aws4SignerUtils.formatTimestamp(requestSigningDateTimeMilli);
    }

    /**
     * @return The clock to use for signing additional data i.e. events or chunks.
     */
    public Clock getSigningClock() {
        return signingClock;
    }

    /**
     * Returns the scope of the request signing.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Returns the formatted date and time of the request signing date in UTC
     * zone.
     */
    public String getFormattedRequestSigningDateTime() {
        return formattedRequestSigningDateTime;
    }

    /**
     * Returns the request signing date time in millis for which the request
     * signature needs to be computed.
     */
    public long getRequestSigningDateTimeMilli() {
        return requestSigningDateTimeMilli;
    }

    /**
     * Returns the AWS region name to be used while computing the signature.
     */
    public String getRegionName() {
        return regionName;
    }

    /**
     * Returns the AWS Service name to be used while computing the signature.
     */
    public String getServiceSigningName() {
        return serviceSigningName;
    }

    /**
     * Returns the formatted date in UTC zone of the signing date for the
     * request.
     */
    public String getFormattedRequestSigningDate() {
        return formattedRequestSigningDate;
    }

    /**
     * Returns the signing algorithm used for computing the signature.
     */
    public String getSigningAlgorithm() {
        return SignerConstant.AWS4_SIGNING_ALGORITHM;
    }

    private Clock resolveSigningClock(Aws4SignerParams signerParams) {
        if (signerParams.signingClockOverride().isPresent()) {
            return signerParams.signingClockOverride().get();
        }
        Clock baseClock = Clock.systemUTC();
        return signerParams.timeOffset()
                .map(offset -> Clock.offset(baseClock, Duration.ofSeconds(-offset)))
                .orElse(baseClock);
    }

    private String getRegion(Region region) {
        return region != null ? region.id() : null;
    }

    /**
     * Returns the scope to be used for the signing.
     */
    private String generateScope(String dateStamp, String serviceName, String regionName) {
        return dateStamp + "/" + regionName + "/" + serviceName + "/" + SignerConstant.AWS4_TERMINATOR;
    }
}
