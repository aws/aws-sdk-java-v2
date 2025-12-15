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
// unoptimized (not sifted) BDD.  optimized booleans/bdd loop.  Method references instead of lambdas.
public final class BddUnOptSubgraph implements ConnectEndpointProvider {


    private static boolean cond0(Registers registers) {
        return (registers.endpoint != null);
    }

    private static boolean cond1(Registers registers) {
        return (registers.region != null);
    }

    private static boolean cond2(Registers registers) {
        return (registers.useFIPS);
    }

    private static boolean cond3(Registers registers) {
        registers.partitionResult = RulesFunctions.awsPartition(registers.region);
        return registers.partitionResult != null;
    }

    private static boolean cond4(Registers registers) {
        return (registers.useDualStack);
    }

    private static boolean cond5(Registers registers) {
        return (registers.partitionResult.supportsFIPS());
    }

    private static boolean cond6(Registers registers) {
        return (registers.partitionResult.supportsDualStack());
    }

    private static boolean cond7(Registers registers) {
        return ("aws-us-gov".equals(registers.partitionResult.name()));
    }

    private static RuleResult result0(Registers registers) {
        return RuleResult.error("Invalid Configuration: FIPS and custom endpoint are not supported");
    }

    private static RuleResult result1(Registers registers) {
        return RuleResult.error("Invalid Configuration: Dualstack and custom endpoint are not supported");
    }

    private static RuleResult result2(Registers registers) {
        return RuleResult.endpoint(Endpoint.builder().url(URI.create(registers.endpoint)).build());
    }

    private static RuleResult result3(Registers registers) {
        return RuleResult
            .endpoint(Endpoint
                          .builder()
                          .url(URI.create("https://connect-fips." + registers.region + "."
                                          + registers.partitionResult.dualStackDnsSuffix())).build());
    }

    private static RuleResult result4(Registers registers) {
        return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
    }

    private static RuleResult result5(Registers registers) {
        return RuleResult.endpoint(Endpoint.builder().url(URI.create("https://connect." + registers.region + ".amazonaws.com"))
                                           .build());
    }

    private static RuleResult result6(Registers registers) {
        return RuleResult.endpoint(Endpoint.builder()
                                           .url(URI.create("https://connect-fips." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                           .build());
    }

    private static RuleResult result7(Registers registers) {
        return RuleResult.error("FIPS is enabled but this partition does not support FIPS");
    }

    private static RuleResult result8(Registers registers) {
        return RuleResult.endpoint(Endpoint.builder()
                                           .url(URI.create("https://connect." + registers.region + "." + registers.partitionResult.dualStackDnsSuffix()))
                                           .build());
    }

    private static RuleResult result9(Registers registers) {
        return RuleResult.error("DualStack is enabled but this partition does not support DualStack");
    }

    private static RuleResult result10(Registers registers) {
        return RuleResult.endpoint(Endpoint.builder()
                                           .url(URI.create("https://connect." + registers.region + "." + registers.partitionResult.dnsSuffix())).build());
    }

    private static RuleResult result11(Registers registers) {
        return RuleResult.error("Invalid Configuration: Missing Region");
    }

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(ConnectEndpointParams params) {
        Registers registers = new Registers();
        registers.region = params.region() == null ? null : params.region().id();
        registers.useDualStack = params.useDualStack();
        registers.useFIPS = params.useFips();
        registers.endpoint = params.endpoint();
        RuleResult result = n13(registers);
        if (result.isError()) {
            String errorMsg = result.error();
            if (errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
                errorMsg += ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.";
            }
            return CompletableFutureUtils.failedFuture(SdkClientException.create(errorMsg));
        }
        return CompletableFuture.completedFuture(result.endpoint());
    }

    private static RuleResult n13(Registers registers) {
        return cond0(registers) ? n1(registers) : n12(registers);
    }
    private static RuleResult n1(Registers registers) {
        return cond2(registers) ? result0(registers) : n0(registers);
    }
    private static RuleResult n0(Registers registers) {
        return cond4(registers) ? result1(registers) : result2(registers);
    }
    private static RuleResult n12(Registers registers) {
        // this is a subgraph: [11, 7, 6, 3, 2, 5, 4, 10, 9, 8]
        if (cond1(registers)) {
            if (cond2(registers)) {
                if (cond3(registers)) {
                    if (cond4(registers)) {
                        if (cond5(registers)) {
                            if (cond6(registers)) {
                                return result3(registers);
                            } else {
                                return result4(registers);
                            }
                        } else {
                            return result4(registers);
                        }
                    } else {
                        if (cond5(registers)) {
                            if (cond7(registers)) {
                                return result5(registers);
                            } else {
                                return result6(registers);
                            }
                        } else {
                            return result7(registers);
                        }
                    }
                } else {
                    return result11(registers);
                }
            } else {
                if (cond3(registers)) {
                    if (cond4(registers)) {
                        if (cond6(registers)) {
                            return result8(registers);
                        } else {
                            return result9(registers);
                        }
                    } else {
                        return result10(registers);
                    }
                } else {
                    return result11(registers);
                }
            }
        } else {
            return result11(registers);
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
