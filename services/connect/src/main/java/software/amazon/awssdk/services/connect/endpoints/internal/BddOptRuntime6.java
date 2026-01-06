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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointParams;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class BddOptRuntime6 implements ConnectEndpointProvider {
    private static final int[] BDD_DEFINITION;

    static {
        try (InputStream in = DefaultConnectEndpointProvider.class.getResourceAsStream("/endpoints_bdd_563d85c5.bin")) {
            if (in == null) {
                throw new IllegalStateException("Resource /endpoints_bdd_563d85c5.bin not found");
            }
            BDD_DEFINITION = new int[42];
            DataInputStream data = new DataInputStream(in);
            for (int i = 0; i < 42; i++) {
                BDD_DEFINITION[i] = data.readInt();
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(ConnectEndpointParams params) {
        Evaluator evaluator = new Evaluator();
        evaluator.region = params.region() == null ? null : params.region().id();
        evaluator.useDualStack = params.useDualStack();
        evaluator.useFIPS = params.useFips();
        evaluator.endpoint = params.endpoint();
        final int[] bdd = BDD_DEFINITION;
        int nodeRef = 2;
        while ((nodeRef > 1 || nodeRef < -1) && nodeRef < 100000000) {
            int base = (Math.abs(nodeRef) - 1) * 3;
            int complemented = nodeRef >> 31 & 1; // 1 if complemented edge, else 0;
            int conditionResult = evaluator.cond(bdd[base]) ? 1 : 0;
            nodeRef = bdd[base + 2 - (complemented ^ conditionResult)];
        }
        if (nodeRef == -1 || nodeRef == 1) {
            return CompletableFutureUtils.failedFuture(SdkClientException
                                                           .create("Rule engine did not reach an error or endpoint result"));
        } else {
            RuleResult result = evaluator.result(nodeRef - 100000001);
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

    private static final class Evaluator {
        String region;

        boolean useDualStack;

        boolean useFIPS;

        String endpoint;

        RulePartition partitionResult;

        public final boolean cond(int i) {
            switch (i) {
                case 0: {
                    return (endpoint != null);
                }
                case 1: {
                    return (region != null);
                }
                case 2: {
                    partitionResult = RulesFunctions.awsPartition(region);
                    return partitionResult != null;
                }
                case 3: {
                    return (useFIPS);
                }
                case 4: {
                    return (partitionResult.supportsFIPS());
                }
                case 5: {
                    return (useDualStack);
                }
                case 6: {
                    return ("aws-us-gov".equals(partitionResult.name()));
                }
                case 7: {
                    return (partitionResult.supportsDualStack());
                }
                default: {
                    throw new IllegalArgumentException("Unknown condition index");
                }
            }
        }

        public final RuleResult result(int i) {
            switch (i) {
                case 0: {
                    return result0();
                }
                case 1: {
                    return result1();
                }
                case 2: {
                    return result2();
                }
                case 3: {
                    return result3();
                }
                case 4: {
                    return result4();
                }
                case 5: {
                    return result5();
                }
                case 6: {
                    return result6();
                }
                case 7: {
                    return result7();
                }
                case 8: {
                    return result8();
                }
                case 9: {
                    return result9();
                }
                case 10: {
                    return result10();
                }
                case 11: {
                    return result11();
                }
                default: {
                    throw new IllegalArgumentException("Unknown condition index");
                }
            }
        }

        private final RuleResult result0() {
            return RuleResult.error("Invalid Configuration: FIPS and custom endpoint are not supported");
        }

        private final RuleResult result1() {
            return RuleResult.error("Invalid Configuration: Dualstack and custom endpoint are not supported");
        }

        private final RuleResult result2() {
            return RuleResult.endpoint(Endpoint.builder().url(URI.create(endpoint)).build());
        }

        private final RuleResult result3() {
            return RuleResult.endpoint(Endpoint.builder()
                                               .url(URI.create("https://connect-fips." + region + "." + partitionResult.dualStackDnsSuffix())).build());
        }

        private final RuleResult result4() {
            return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
        }

        private final RuleResult result5() {
            return RuleResult
                .endpoint(Endpoint.builder().url(URI.create("https://connect." + region + ".amazonaws.com")).build());
        }

        private final RuleResult result6() {
            return RuleResult.endpoint(Endpoint.builder()
                                               .url(URI.create("https://connect-fips." + region + "." + partitionResult.dnsSuffix())).build());
        }

        private final RuleResult result7() {
            return RuleResult.error("FIPS is enabled but this partition does not support FIPS");
        }

        private final RuleResult result8() {
            return RuleResult.endpoint(Endpoint.builder()
                                               .url(URI.create("https://connect." + region + "." + partitionResult.dualStackDnsSuffix())).build());
        }

        private final RuleResult result9() {
            return RuleResult.error("DualStack is enabled but this partition does not support DualStack");
        }

        private final RuleResult result10() {
            return RuleResult.endpoint(Endpoint.builder()
                                               .url(URI.create("https://connect." + region + "." + partitionResult.dnsSuffix())).build());
        }

        private final RuleResult result11() {
            return RuleResult.error("Invalid Configuration: Missing Region");
        }
    }

    public static class DynamicAuthBuilder {
        String name;

        private Map<String, String> properties = new HashMap<>();

        public static DynamicAuthBuilder builder() {
            return new DynamicAuthBuilder();
        }

        DynamicAuthBuilder name(String name) {
            this.name = name;
            return this;
        }

        DynamicAuthBuilder property(String key, String value) {
            properties.put(key, value);
            return this;
        }

        public EndpointAuthScheme build() {
            return null;
        }
    }
}
