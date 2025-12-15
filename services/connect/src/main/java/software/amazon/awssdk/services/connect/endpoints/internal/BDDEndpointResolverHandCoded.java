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

package software.amazon.awssdk.services.connect.endpoints.internal;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointParams;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointProvider;

public class BDDEndpointResolverHandCoded implements ConnectEndpointProvider {

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(ConnectEndpointParams endpointParams) {
        return CompletableFuture.completedFuture(resolveBDD(endpointParams));
    }

    private Endpoint resolveBDD(ConnectEndpointParams endpointParams) {
        // C0: isSet(Endpoint)
        if (endpointParams.endpoint() != null) {
            // node 12
            if (endpointParams.useFips()) {
                throw SdkClientException.create("Error: Invalid Configuration: FIPS and custom endpoint are not supported"); // R1
            } else {
                if (endpointParams.useDualStack()) {
                    throw SdkClientException.create("Error: Invalid Configuration: Dualstack and custom endpoint are not "
                                                    + "supported"); // R2
                } else {
                    return Endpoint.builder().url(URI.create(endpointParams.endpoint())).build(); // R3
                }
            }
        } else {
            // node 2: C1: isSet(Region)
            Region region = endpointParams.region();
            if (region == null) {
                throw SdkClientException.create("Invalid Configuration: Missing Region"); // R12
            }

            // node 3: C2: PartitionResult = aws.partition(Region)
            RulePartition partitionResult = RulesFunctions.awsPartition(region.id());

            // node 4: C3: booleanEquals(UseFIPS, true)
            if (endpointParams.useFips()) {
                // node 7: C4: booleanEquals(UseDualStack, true)
                if (endpointParams.useDualStack()) {
                    // node 10: C5: partition.supportsDualStack
                    if (partitionResult.supportsDualStack()) {
                        // node 11: C6: partition.supportsFIPS
                        if (partitionResult.supportsFIPS()) {
                            return Endpoint.builder()
                                           .url(URI.create(String.format(
                                               "https://connect-fips.%s.%s",
                                               region.id(),
                                               partitionResult.dualStackDnsSuffix())))
                                           .build(); // R4
                        } else {
                            throw SdkClientException.create("FIPS and DualStack are enabled, but this partition does not support one or both"); // R5
                        }
                    } else {
                        throw SdkClientException.create("FIPS and DualStack are enabled, but this partition does not support one or both"); // R5
                    }
                } else {
                    // node 8: C6: partition.supportsFIPS
                    if (partitionResult.supportsFIPS()) {
                        // node 9: C7: stringEquals("aws-us-gov", PartitionResult#name)
                        if ("aws-us-gov".equals(partitionResult.name())) {
                            return Endpoint.builder()
                                           .url(URI.create(String.format(
                                               "https://connect.%s.amazonaws.com",
                                               region.id())))
                                           .build(); // R6
                        } else {
                            return Endpoint.builder()
                                           .url(URI.create(String.format(
                                               "https://connect-fips.%s.%s",
                                               region.id(),
                                               partitionResult.dnsSuffix())))
                                           .build(); // R7
                        }
                    } else {
                        throw SdkClientException.create("FIPS is enabled but this partition does not support FIPS"); // R8
                    }
                }
            } else {
                // node 5: C4: booleanEquals(UseDualStack, true)
                if (endpointParams.useDualStack()) {
                    // node 6: C5: partition.supportsDualStack
                    if (partitionResult.supportsDualStack()) {
                        return Endpoint.builder()
                                       .url(URI.create(String.format(
                                           "https://connect.%s.%s",
                                           region.id(),
                                           partitionResult.dualStackDnsSuffix())))
                                       .build(); // R9
                    } else {
                        throw SdkClientException.create("DualStack is enabled but this partition does not support DualStack"); // R10
                    }
                } else {
                    return Endpoint.builder()
                                   .url(URI.create(String.format(
                                       "https://connect.%s.%s",
                                       region.id(),
                                       partitionResult.dnsSuffix())))
                                   .build(); // R11
                }
            }
        }
    }
}
