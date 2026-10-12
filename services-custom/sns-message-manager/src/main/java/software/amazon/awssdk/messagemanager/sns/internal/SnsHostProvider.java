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

package software.amazon.awssdk.messagemanager.sns.internal;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.PartitionMetadata;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.endpoints.SnsEndpointParams;
import software.amazon.awssdk.services.sns.endpoints.SnsEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Utility class for determining both the regional endpoint that SNS certificates are expected to be hosted from, as well as the
 * expected common name (CN) that the certificate from that endpoint must have.
 */
@SdkInternalApi
@ThreadSafe
public class SnsHostProvider {
    private static final Logger LOG = Logger.loggerFor(SnsHostProvider.class);

    private final Region region;
    private final SnsEndpointProvider endpointProvider;

    public SnsHostProvider(Region region) {
        this(region, SnsEndpointProvider.defaultProvider());
    }

    @SdkTestInternalApi
    SnsHostProvider(Region region, SnsEndpointProvider endpointProvider) {
        Validate.notNull(region, "region must not be null");
        Validate.notNull(endpointProvider, "endpointProvider must not be null");
        this.region = region;
        this.endpointProvider = endpointProvider;
    }

    public URI regionalEndpoint() {
        SnsEndpointParams params = SnsEndpointParams.builder().region(region).build();
        try {
            Endpoint endpoint = CompletableFutureUtils.joinLikeSync(endpointProvider.resolveEndpoint(params));
            URI url = endpoint.url();
            LOG.debug(() -> String.format("Resolved endpoint %s for region %s", url, region));
            return url;
        } catch (SdkClientException e) {
            throw SdkClientException.create("Unable to resolve SNS endpoint for region " + region, e);
        }
    }

    public String signingCertCommonName() {
        String commonName = signingCertCommonNameInternal();
        LOG.debug(() -> String.format("Resolved common name %s for region %s", commonName, region));
        return commonName;
    }

    private String signingCertCommonNameInternal() {
        // If we don't know about this region, try to guess common name
        if (!Region.regions().contains(region)) {
            // Find the partition where it belongs by checking the region against the published pattern for known partitions.
            // e.g. 'us-gov-west-3' would match the 'aws-us-gov' partition.
            // This will return the 'aws' partition if it fails to match any partition.
            PartitionMetadata partitionMetadata = PartitionMetadata.of(region);
            return "sns." + partitionMetadata.dnsSuffix();
        }

        String regionId = region.id();

        switch (regionId) {
            case "cn-north-1":
                return "sns-cn-north-1.amazonaws.com.cn";
            case "cn-northwest-1":
                return "sns-cn-northwest-1.amazonaws.com.cn";
            case "us-gov-west-1":
            case "us-gov-east-1":
                return "sns-us-gov-west-1.amazonaws.com";
            case "us-iso-east-1":
                return "sns-us-iso-east-1.c2s.ic.gov";
            case "us-isob-east-1":
                return "sns-us-isob-east-1.sc2s.sgov.gov";
            case "us-isof-east-1":
                return "sns-signing.us-isof-east-1.csp.hci.ic.gov";
            case "us-isof-south-1":
                return "sns-signing.us-isof-south-1.csp.hci.ic.gov";
            case "eu-isoe-west-1":
                return "sns-signing.eu-isoe-west-1.cloud.adc-e.uk";
            case "eusc-de-east-1":
                return "sns-signing.eusc-de-east-1.amazonaws.eu";
            case "ap-east-1":
            case "ap-east-2":
            case "ap-south-2":
            case "ap-southeast-5":
            case "ap-southeast-6":
            case "ap-southeast-7":
            case "me-south-1":
            case "me-central-1":
            case "eu-south-1":
            case "eu-south-2":
            case "eu-central-2":
            case "af-south-1":
            case "ap-southeast-3":
            case "ap-southeast-4":
            case "il-central-1":
            case "ca-west-1":
            case "mx-central-1":
                return "sns-signing." + regionId + ".amazonaws.com";
            default:
                return "sns.amazonaws.com";
        }
    }
}
