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
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointParams;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
// unoptimized (not sifted) BDD.  optimized booleans/bdd loop.  Lambdas
public final class BddUnOptRuntime3 implements ConnectEndpointProvider {
    private static final int[] BDD_DEFINITION = { -1, 1, -1, 4, 100000002, 100000003, 2, 100000001, 2, 6, 100000004, 100000005,
                                                  5, 4, 100000005, 7, 100000006, 100000007, 5, 6, 100000008, 4, 5, 7, 3, 8, 100000012, 6, 100000009, 100000010, 4, 10,
                                                  100000011, 3, 11, 100000012, 2, 9, 12, 1, 13, 100000012, 0, 3, 14 };

    private static final ConditionFn[] CONDITION_FNS = {
        // condition 0
        (registers) -> {
            return (registers.endpoint != null);
        }, // condition 1
        (registers) -> {
            return (registers.region != null);
        }, // condition 2
        (registers) -> {
            return (registers.useFIPS);
        }, // condition 3, assign PartitionResult
        (registers) -> {
            registers.partitionResult = RulesFunctions.awsPartition(registers.region);
            return registers.partitionResult != null;
        }, // condition 4
        (registers) -> {
            return (registers.useDualStack);
        }, // condition 5
        (registers) -> {
            return (registers.partitionResult.supportsFIPS());
        }, // condition 6
        (registers) -> {
            return (registers.partitionResult.supportsDualStack());
        }, // condition 7
        (registers) -> {
            return ("aws-us-gov".equals(registers.partitionResult.name()));
        }

    };

    private static final ResultFn[] RESULT_FNS = {
        (registers) -> {
            return RuleResult.error("Invalid Configuration: FIPS and custom endpoint are not supported");
        },
        (registers) -> {
            return RuleResult.error("Invalid Configuration: Dualstack and custom endpoint are not supported");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint.builder().url(URI.create(registers.endpoint)).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://connect-fips." + registers.region + "."
                                                           + registers.partitionResult.dualStackDnsSuffix())).build());
        },
        (registers) -> {
            return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint.builder()
                                               .url(URI.create("https://connect." + registers.region + ".amazonaws.com")).build());
        },
        (registers) -> {
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .url(URI.create("https://connect-fips." + registers.region + "."
                                              + registers.partitionResult.dnsSuffix())).build());
        },
        (registers) -> {
            return RuleResult.error("FIPS is enabled but this partition does not support FIPS");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://connect." + registers.region + "."
                                                           + registers.partitionResult.dualStackDnsSuffix())).build());
        },
        (registers) -> {
            return RuleResult.error("DualStack is enabled but this partition does not support DualStack");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint.builder()
                                               .url(URI.create("https://connect." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                               .build());
        }, (registers) -> {
        return RuleResult.error("Invalid Configuration: Missing Region");
    }

    };

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(ConnectEndpointParams params) {
        Registers registers = new Registers();
        registers.region = params.region() == null ? null : params.region().id();
        registers.useDualStack = params.useDualStack();
        registers.useFIPS = params.useFips();
        registers.endpoint = params.endpoint();
        final int[] bdd = BDD_DEFINITION;
        int nodeRef = 15;
        while ((nodeRef > 1 || nodeRef < -1) && nodeRef < 100000000) {
            int base = (Math.abs(nodeRef) - 1) * 3;
            int complemented = nodeRef >> 31 & 1; // 1 if complemented edge, else 0;
            int conditionResult = CONDITION_FNS[bdd[base]].test(registers) ? 1 : 0;
            nodeRef = bdd[base + 2 - (complemented ^ conditionResult)];
        }
        if (nodeRef == -1 || nodeRef == 1) {
            return CompletableFutureUtils.failedFuture(SdkClientException
                                                           .create("Rule engine did not reach an error or endpoint result"));
        } else {
            RuleResult result = RESULT_FNS[nodeRef - 100000001].apply(registers);
            if (result.isError()) {
                String errorMsg = result.error();
                if (errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
                    errorMsg += ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.";
                }
                return CompletableFutureUtils.failedFuture(SdkClientException.create(errorMsg));
            }
            return CompletableFuture.completedFuture(result.endpoint());
        }
    }

    private static class Registers {
        String region;

        boolean useDualStack;

        boolean useFIPS;

        String endpoint;

        RulePartition partitionResult;
    }

    @FunctionalInterface
    interface ConditionFn {
        boolean test(Registers registers);
    }

    @FunctionalInterface
    interface ResultFn {
        RuleResult apply(Registers registers);
    }
}
