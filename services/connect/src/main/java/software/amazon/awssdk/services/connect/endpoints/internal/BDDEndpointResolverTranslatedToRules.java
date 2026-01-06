/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.connect.endpoints.internal;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointParams;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * BDD
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class BDDEndpointResolverTranslatedToRules implements ConnectEndpointProvider {
    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(ConnectEndpointParams params) {
        Validate.notNull(params.useDualStack(), "Parameter 'UseDualStack' must not be null");
        Validate.notNull(params.useFips(), "Parameter 'UseFIPS' must not be null");
        try {
            Region region = params.region();
            String regionId = region == null ? null : region.id();
            RuleResult result = endpointRule0(params, regionId);
            if (result.canContinue()) {
                throw SdkClientException.create("Rule engine did not reach an error or endpoint result");
            }
            if (result.isError()) {
                String errorMsg = result.error();
                if (errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
                    errorMsg += ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.";
                }
                throw SdkClientException.create(errorMsg);
            }
            return CompletableFuture.completedFuture(result.endpoint());
        } catch (Exception error) {
            return CompletableFutureUtils.failedFuture(error);
        }
    }

    private static RuleResult endpointRule0(ConnectEndpointParams params, String region) {
        RuleResult result = endpointRule1(params);
        if (result.isResolved()) {
            return result;
        }
        return endpointRule7(params, region);
    }

    private static RuleResult endpointRule1(ConnectEndpointParams params) {
        if (params.endpoint() != null) {
            return endpointRule2(params);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule2(ConnectEndpointParams params) {
        if (params.useFips()) {
            return RuleResult.error("Invalid Configuration: FIPS and custom endpoint are not supported");
        }
        return endpointRule4(params);
    }

    private static RuleResult endpointRule4(ConnectEndpointParams params) {
        if (params.useDualStack()) {
            return RuleResult.error("Invalid Configuration: Dualstack and custom endpoint are not supported");
        }
        return RuleResult.endpoint(Endpoint.builder().url(URI.create(params.endpoint())).build());
    }

    private static RuleResult endpointRule7(ConnectEndpointParams params, String region) {
        RuleResult result = endpointRule8(params, region);
        if (result.isResolved()) {
            return result;
        }
        return RuleResult.error("Invalid Configuration: Missing Region");
    }

    private static RuleResult endpointRule8(ConnectEndpointParams params, String region) {
        if (region != null) {
            return endpointRule9(params, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule9(ConnectEndpointParams params, String region) {
        RuleResult result = endpointRule10(params, region);
        if (result.isResolved()) {
            return result;
        }
        return endpointRule28(params, region);
    }

    private static RuleResult endpointRule10(ConnectEndpointParams params, String region) {
        if (params.useFips()) {
            return endpointRule11(params, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule11(ConnectEndpointParams params, String region) {
        RuleResult result = endpointRule12(params, region);
        if (result.isResolved()) {
            return result;
        }
        return RuleResult.error("Invalid Configuration: Missing Region");
    }

    private static RuleResult endpointRule12(ConnectEndpointParams params, String region) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            return endpointRule13(params, partitionResult, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule13(ConnectEndpointParams params, RulePartition partitionResult, String region) {
        RuleResult result = endpointRule14(params, partitionResult, region);
        if (result.isResolved()) {
            return result;
        }
        return endpointRule21(params, partitionResult, region);
    }

    private static RuleResult endpointRule14(ConnectEndpointParams params, RulePartition partitionResult, String region) {
        if (params.useDualStack()) {
            return endpointRule15(params, partitionResult, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule15(ConnectEndpointParams params, RulePartition partitionResult, String region) {
        RuleResult result = endpointRule16(params, partitionResult, region);
        if (result.isResolved()) {
            return result;
        }
        return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
    }

    private static RuleResult endpointRule16(ConnectEndpointParams params, RulePartition partitionResult, String region) {
        if (partitionResult.supportsFIPS()) {
            return endpointRule17(params, partitionResult, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule17(ConnectEndpointParams params, RulePartition partitionResult, String region) {
        if (partitionResult.supportsDualStack()) {
            return RuleResult.endpoint(Endpoint.builder()
                                               .url(URI.create("https://connect-fips." + region + "." + partitionResult.dualStackDnsSuffix())).build());
        }
        return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
    }

    private static RuleResult endpointRule21(ConnectEndpointParams params, RulePartition partitionResult, String region) {
        RuleResult result = endpointRule22(params, partitionResult, region);
        if (result.isResolved()) {
            return result;
        }
        return RuleResult.error("FIPS is enabled but this partition does not support FIPS");
    }

    private static RuleResult endpointRule22(ConnectEndpointParams params, RulePartition partitionResult, String region) {
        if (partitionResult.supportsFIPS()) {
            return endpointRule23(params, partitionResult, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule23(ConnectEndpointParams params, RulePartition partitionResult, String region) {
        if ("aws-us-gov".equals(partitionResult.name())) {
            return RuleResult.endpoint(Endpoint.builder().url(URI.create("https://connect." + region + ".amazonaws.com")).build());
        }
        return RuleResult.endpoint(Endpoint.builder()
                                           .url(URI.create("https://connect-fips." + region + "." + partitionResult.dnsSuffix())).build());
    }

    private static RuleResult endpointRule28(ConnectEndpointParams params, String region) {
        RuleResult result = endpointRule29(params, region);
        if (result.isResolved()) {
            return result;
        }
        return RuleResult.error("Invalid Configuration: Missing Region");
    }

    private static RuleResult endpointRule29(ConnectEndpointParams params, String region) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            return endpointRule30(params, partitionResult, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule30(ConnectEndpointParams params, RulePartition partitionResult, String region) {
        RuleResult result = endpointRule31(params, partitionResult, region);
        if (result.isResolved()) {
            return result;
        }
        return RuleResult.endpoint(Endpoint.builder()
                                           .url(URI.create("https://connect." + region + "." + partitionResult.dnsSuffix())).build());
    }

    private static RuleResult endpointRule31(ConnectEndpointParams params, RulePartition partitionResult, String region) {
        if (params.useDualStack()) {
            return endpointRule32(params, partitionResult, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule32(ConnectEndpointParams params, RulePartition partitionResult, String region) {
        if (partitionResult.supportsDualStack()) {
            return RuleResult.endpoint(Endpoint.builder()
                                               .url(URI.create("https://connect." + region + "." + partitionResult.dualStackDnsSuffix())).build());
        }
        return RuleResult.error("DualStack is enabled but this partition does not support DualStack");
    }

    @Override
    public boolean equals(Object rhs) {
        return rhs != null && getClass().equals(rhs.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
