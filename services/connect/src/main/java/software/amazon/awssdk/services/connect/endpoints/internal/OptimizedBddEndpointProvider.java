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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointUrl;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointParams;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class OptimizedBddEndpointProvider implements ConnectEndpointProvider {
    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(ConnectEndpointParams params) {
        Evaluator evaluator = new Evaluator();
        evaluator.region = params.region() == null ? null : params.region().id();
        evaluator.params = params;
        RuleResult result = evaluator.evaluate();
        if (result.isEndpoint()) {
            return CompletableFuture.completedFuture(result.endpoint());
        }
        if (result.isError()) {
            String errorMsg = result.error();
            if (errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
                errorMsg += ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.";
            }
            return CompletableFutureUtils.failedFuture(SdkClientException.create(errorMsg));
        }
        return CompletableFutureUtils.failedFuture(SdkClientException
                .create("Rule engine did not reach an error or endpoint result"));
    }

    private static final class Evaluator {
        ConnectEndpointParams params;

        String region;

        RulePartition partitionResult;

        public RuleResult evaluate() {
            if (cond0()) {
                if (cond3()) {
                    return result0();
                }
                if (cond4()) {
                    return result1();
                }
                return result2();
            }
            if (cond1()) {
                if (cond2()) {
                    if (cond3()) {
                        if (cond4()) {
                            if (cond5()) {
                                if (cond6()) {
                                    return result3();
                                }
                                return result4();
                            }
                            return result4();
                        }
                        if (cond6()) {
                            if (cond7()) {
                                return result5();
                            }
                            return result6();
                        }
                        return result7();
                    }
                    if (cond4()) {
                        if (cond5()) {
                            return result8();
                        }
                        return result9();
                    }
                    return result10();
                }
                return result11();
            }
            return result11();
        }

        private boolean cond0() {
            return (params.endpoint() != null);
        }

        private boolean cond1() {
            return (region != null);
        }

        private boolean cond2() {
            partitionResult = RulesFunctions.awsPartition(region);
            return partitionResult != null;
        }

        private boolean cond3() {
            return (params.useFips());
        }

        private boolean cond4() {
            return (params.useDualStack());
        }

        private boolean cond5() {
            return (partitionResult.supportsDualStack());
        }

        private boolean cond6() {
            return (partitionResult.supportsFIPS());
        }

        private boolean cond7() {
            return ("aws-us-gov".equals(partitionResult.name()));
        }

        private RuleResult result0() {
            return RuleResult.error("Invalid Configuration: FIPS and custom endpoint are not supported");
        }

        private RuleResult result1() {
            return RuleResult.error("Invalid Configuration: Dualstack and custom endpoint are not supported");
        }

        private RuleResult result2() {
            return RuleResult.endpoint(Endpoint.builder().endpointUrl(EndpointUrl.fromString(params.endpoint())).build());
        }

        private RuleResult result3() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    "connect-fips." + region + "." + partitionResult.dualStackDnsSuffix(), -1, "")).build());
        }

        private RuleResult result4() {
            return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
        }

        private RuleResult result5() {
            return RuleResult.endpoint(Endpoint.builder()
                    .endpointUrl(EndpointUrl.fromComponents("https", "connect." + region + ".amazonaws.com", -1, "")).build());
        }

        private RuleResult result6() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "connect-fips." + region + "." + partitionResult.dnsSuffix(), -1,
                                    "")).build());
        }

        private RuleResult result7() {
            return RuleResult.error("FIPS is enabled but this partition does not support FIPS");
        }

        private RuleResult result8() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "connect." + region + "." + partitionResult.dualStackDnsSuffix(),
                                    -1, "")).build());
        }

        private RuleResult result9() {
            return RuleResult.error("DualStack is enabled but this partition does not support DualStack");
        }

        private RuleResult result10() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "connect." + region + "." + partitionResult.dnsSuffix(), -1, ""))
                    .build());
        }

        private RuleResult result11() {
            return RuleResult.error("Invalid Configuration: Missing Region");
        }
    }
}
