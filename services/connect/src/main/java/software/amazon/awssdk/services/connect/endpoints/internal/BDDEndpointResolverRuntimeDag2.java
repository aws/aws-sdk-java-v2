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
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class BDDEndpointResolverRuntimeDag2 implements ConnectEndpointProvider {
    private static final int[] BDD_DEFINITION = { -1, 1, -1, 4, 100000002, 100000003, 2, 100000001, 2, 6, 100000004, 100000005,
                                                  5, 4, 100000005, 7, 100000006, 100000007, 5, 6, 100000008, 4, 5, 7, 3, 8, 100000012, 6, 100000009, 100000010, 4, 10,
                                                  100000011, 3, 11, 100000012, 2, 9, 12, 1, 13, 100000012, 0, 3, 14 };

    public static class Registers {
        String region;
        Boolean useDualStack;
        Boolean useFips;
        String endpoint;
        RulePartition partition;
    }

    @FunctionalInterface
    public interface ConditionFn2 {
        boolean test(Registers registers);
    }

    @FunctionalInterface
    public interface ResultFn2 {
        RuleResult apply(Registers registers);
    }


    private static final ConditionFn2[] CONDITION_FNS = {
        (registers) -> {
            return (registers.endpoint != null);
        },
        (registers) -> {
            return (( registers.region) != null);
        },
        (registers) -> {
            return (((Boolean) registers.useFips));
        },
        (registers) -> {
            registers.partition = RulesFunctions.awsPartition(( registers.region));
            return registers.partition != null;
        },
        (registers) -> {
            return (((Boolean) registers.useDualStack));
        },
        (registers) -> {
            return (( registers.partition).supportsFIPS());
        },
        (registers) -> {
            return (( registers.partition).supportsDualStack());
        },
        (registers) -> {
            return ("aws-us-gov".equals(( registers.partition)
                                            .name()));
        }

    };

    private static final ResultFn2[] RESULT_FNS = {
        (registers) -> {
            return RuleResult.error("Invalid Configuration: FIPS and custom endpoint are not supported");
        },
        (registers) -> {
            return RuleResult.error("Invalid Configuration: Dualstack and custom endpoint are not supported");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint.builder().url(URI.create((registers.endpoint))).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://acm-fips."
                                                           + ( registers.region)
                                                           + "."
                                                           + ( registers.partition)
                                                               .dualStackDnsSuffix())).build());
        },
        (registers) -> {
            return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint.builder()
                                               .url(URI.create("https://acm." + ( registers.region) + ".amazonaws.com")).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://acm-fips."
                                                           + (registers.region)
                                                           + "."
                                                           + ( registers.partition)
                                                               .dnsSuffix())).build());
        },
        (registers) -> {
            return RuleResult.error("FIPS is enabled but this partition does not support FIPS");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://acm."
                                                           + ( registers.region)
                                                           + "."
                                                           + ( registers.partition)
                                                               .dualStackDnsSuffix())).build());
        },
        (registers) -> {
            return RuleResult.error("DualStack is enabled but this partition does not support DualStack");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://acm."
                                                           + ( registers.region)
                                                           + "."
                                                           + ( registers.partition)
                                                               .dnsSuffix())).build());
        }, (registers) -> {
        return RuleResult.error("Invalid Configuration: Missing Region");
    }

    };

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(ConnectEndpointParams params) {
        try {
            Validate.notNull(params.useDualStack(), "Parameter 'UseDualStack' must not be null");
            Validate.notNull(params.useFips(), "Parameter 'UseFIPS' must not be null");
            Registers registers = new Registers();
            registers.region = params.region() == null ? null : params.region().id();
            registers.useDualStack = params.useDualStack();
            registers.useFips = params.useFips();
            registers.endpoint= params.endpoint();
            int nodeRef = 15;
            while (nodeRef != 1 && nodeRef != -1 && nodeRef < 100000000) {
                boolean complemented = nodeRef < 0;
                int nodeI = java.lang.Math.abs(nodeRef) - 1;
                boolean conditionResult = CONDITION_FNS[BDD_DEFINITION[nodeI * 3]].test(registers);
                if (complemented == conditionResult) {
                    nodeRef = BDD_DEFINITION[nodeI * 3 + 2];
                } else {
                    nodeRef = BDD_DEFINITION[nodeI * 3 + 1];
                }
            }
            if (nodeRef == -1 || nodeRef == 1) {
                throw SdkClientException.create("Rule engine did not reach an error or endpoint result");
            } else {
                RuleResult result = RESULT_FNS[nodeRef - 100000001].apply(registers);
                if (result.isError()) {
                    String errorMsg = result.error();
                    if (errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
                        errorMsg += ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.";
                    }
                    throw SdkClientException.create(errorMsg);
                }
                return CompletableFuture.completedFuture(result.endpoint());
            }
        } catch (Exception error) {
            return CompletableFutureUtils.failedFuture(error);
        }
    }
}
