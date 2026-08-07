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

package software.amazon.awssdk.services.dynamodb.endpoints.internal;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointUrl;
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointParams;
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class OptimizedBddEndpointProvider implements DynamoDbEndpointProvider {
    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(DynamoDbEndpointParams params) {
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
        DynamoDbEndpointParams params;

        String region;

        RulePartition partitionResult;

        RuleArn parsedArn_ssa_2;

        String firstArn;

        RuleArn parsedArn_ssa_1;

        public RuleResult evaluate() {
            if (cond0()) {
                if (cond1()) {
                    if (cond2()) {
                        return result0();
                    }
                    if (cond3()) {
                        if (cond6()) {
                            return result1();
                        }
                        if (cond8()) {
                            return result2();
                        }
                        return result3();
                    }
                    return node64();
                }
                if (cond2()) {
                    if (cond3()) {
                        if (cond4()) {
                            return result4();
                        }
                        if (cond5()) {
                            if (cond6()) {
                                if (cond7()) {
                                    if (cond9()) {
                                        if (cond26()) {
                                            return result7();
                                        }
                                        return result8();
                                    }
                                    return result8();
                                }
                                return result9();
                            }
                            if (cond9()) {
                                if (cond26()) {
                                    return result7();
                                }
                                return node58();
                            }
                            return node58();
                        }
                        if (cond6()) {
                            return result9();
                        }
                        return result12();
                    }
                    return result24();
                }
                if (cond3()) {
                    if (cond4()) {
                        if (cond6()) {
                            return result5();
                        }
                        return result6();
                    }
                    if (cond6()) {
                        if (cond7()) {
                            if (cond9()) {
                                if (cond10()) {
                                    if (cond11()) {
                                        return node49();
                                    }
                                    if (cond12()) {
                                        if (cond13()) {
                                            if (cond14()) {
                                                if (cond15()) {
                                                    if (cond16()) {
                                                        if (cond17()) {
                                                            return result13();
                                                        }
                                                        return node40();
                                                    }
                                                    return node40();
                                                }
                                                return node40();
                                            }
                                            return node40();
                                        }
                                        return node40();
                                    }
                                    return node40();
                                }
                                if (cond26()) {
                                    return result18();
                                }
                                return result19();
                            }
                            return result19();
                        }
                        return result20();
                    }
                    if (cond9()) {
                        if (cond10()) {
                            if (cond11()) {
                                return node28();
                            }
                            if (cond12()) {
                                if (cond13()) {
                                    if (cond14()) {
                                        if (cond15()) {
                                            if (cond16()) {
                                                if (cond17()) {
                                                    return result21();
                                                }
                                                return node19();
                                            }
                                            return node19();
                                        }
                                        return node19();
                                    }
                                    return node19();
                                }
                                return node19();
                            }
                            return node19();
                        }
                        if (cond26()) {
                            return result18();
                        }
                        return result10();
                    }
                    return result10();
                }
                return result24();
            }
            if (cond1()) {
                if (cond2()) {
                    return result0();
                }
                return node64();
            }
            return result24();
        }

        private RuleResult node64() {
            if (cond6()) {
                return result1();
            }
            return result3();
        }

        private RuleResult node58() {
            if (cond28()) {
                return result10();
            }
            return result11();
        }

        private RuleResult node49() {
            if (cond26()) {
                return result17();
            }
            return result19();
        }

        private RuleResult node40() {
            if (cond18()) {
                if (cond19()) {
                    if (cond20()) {
                        if (cond21()) {
                            if (cond22()) {
                                if (cond23()) {
                                    if (cond24()) {
                                        return result14();
                                    }
                                    return node47();
                                }
                                return node47();
                            }
                            return node47();
                        }
                        return node47();
                    }
                    return node47();
                }
                return node47();
            }
            return node47();
        }

        private RuleResult node28() {
            if (cond26()) {
                return result17();
            }
            return result10();
        }

        private RuleResult node19() {
            if (cond18()) {
                if (cond19()) {
                    if (cond20()) {
                        if (cond21()) {
                            if (cond22()) {
                                if (cond23()) {
                                    if (cond24()) {
                                        return result22();
                                    }
                                    return node26();
                                }
                                return node26();
                            }
                            return node26();
                        }
                        return node26();
                    }
                    return node26();
                }
                return node26();
            }
            return node26();
        }

        private RuleResult node47() {
            if (cond25()) {
                if (cond27()) {
                    return result15();
                }
                return result16();
            }
            return node49();
        }

        private RuleResult node26() {
            if (cond25()) {
                if (cond27()) {
                    return result23();
                }
                return result16();
            }
            return node28();
        }

        private boolean cond0() {
            return (region != null);
        }

        private boolean cond1() {
            return (params.endpoint() != null);
        }

        private boolean cond2() {
            return (params.useFips());
        }

        private boolean cond3() {
            partitionResult = RulesFunctions.awsPartition(region);
            return partitionResult != null;
        }

        private boolean cond4() {
            return ("local".equals(region));
        }

        private boolean cond5() {
            return (partitionResult.supportsFIPS());
        }

        private boolean cond6() {
            return (params.useDualStack());
        }

        private boolean cond7() {
            return (partitionResult.supportsDualStack());
        }

        private boolean cond8() {
            return (RulesFunctions.stringEquals("https://dynamodb." + region + "." + partitionResult.dualStackDnsSuffix(),
                    params.endpoint()));
        }

        private boolean cond9() {
            return (params.accountIdEndpointMode() != null);
        }

        private boolean cond10() {
            return ("aws".equals(partitionResult.name()));
        }

        private boolean cond11() {
            return ("disabled".equals(params.accountIdEndpointMode()));
        }

        private boolean cond12() {
            return (params.resourceArn() != null);
        }

        private boolean cond13() {
            parsedArn_ssa_2 = RulesFunctions.awsParseArn(params.resourceArn());
            return parsedArn_ssa_2 != null;
        }

        private boolean cond14() {
            return (RulesFunctions.stringEquals(parsedArn_ssa_2.region(), region));
        }

        private boolean cond15() {
            return ("dynamodb".equals(parsedArn_ssa_2.service()));
        }

        private boolean cond16() {
            return (RulesFunctions.isValidHostLabel(parsedArn_ssa_2.accountId(), false));
        }

        private boolean cond17() {
            return (RulesFunctions.isValidHostLabel(parsedArn_ssa_2.region(), false));
        }

        private boolean cond18() {
            return (params.resourceArnList() != null);
        }

        private boolean cond19() {
            firstArn = RulesFunctions.listAccess(params.resourceArnList(), 0);
            return firstArn != null;
        }

        private boolean cond20() {
            parsedArn_ssa_1 = RulesFunctions.awsParseArn(firstArn);
            return parsedArn_ssa_1 != null;
        }

        private boolean cond21() {
            return (RulesFunctions.stringEquals(parsedArn_ssa_1.region(), region));
        }

        private boolean cond22() {
            return ("dynamodb".equals(parsedArn_ssa_1.service()));
        }

        private boolean cond23() {
            return (RulesFunctions.isValidHostLabel(parsedArn_ssa_1.accountId(), false));
        }

        private boolean cond24() {
            return (RulesFunctions.isValidHostLabel(parsedArn_ssa_1.region(), false));
        }

        private boolean cond25() {
            return (params.accountId() != null);
        }

        private boolean cond26() {
            return ("required".equals(params.accountIdEndpointMode()));
        }

        private boolean cond27() {
            return (RulesFunctions.isValidHostLabel(params.accountId(), false));
        }

        private boolean cond28() {
            return ("aws-us-gov".equals(partitionResult.name()));
        }

        private RuleResult result0() {
            return RuleResult.error("Invalid Configuration: FIPS and custom endpoint are not supported");
        }

        private RuleResult result1() {
            return RuleResult.error("Invalid Configuration: Dualstack and custom endpoint are not supported");
        }

        private RuleResult result2() {
            return RuleResult
                    .error("Endpoint override is not supported for dual-stack endpoints. Please enable dual-stack functionality by enabling the configuration. For more details, see: https://docs.aws.amazon.com/sdkref/latest/guide/feature-endpoints.html");
        }

        private RuleResult result3() {
            return RuleResult.endpoint(Endpoint.builder().endpointUrl(EndpointUrl.fromString(params.endpoint())).build());
        }

        private RuleResult result4() {
            return RuleResult.error("Invalid Configuration: FIPS and local endpoint are not supported");
        }

        private RuleResult result5() {
            return RuleResult.error("Invalid Configuration: Dualstack and local endpoint are not supported");
        }

        private RuleResult result6() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(EndpointUrl.fromComponents("http", "localhost", 8000, ""))
                    .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().signingName("dynamodb").signingRegion("us-east-1").build()))
                    .build());
        }

        private RuleResult result7() {
            return RuleResult
                    .error("Invalid Configuration: AccountIdEndpointMode is required and FIPS is enabled, but FIPS account endpoints are not supported");
        }

        private RuleResult result8() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    "dynamodb-fips." + region + "." + partitionResult.dualStackDnsSuffix(), -1, "")).build());
        }

        private RuleResult result9() {
            return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
        }

        private RuleResult result10() {
            return RuleResult
                    .endpoint(Endpoint
                            .builder()
                            .endpointUrl(
                                    EndpointUrl.fromComponents("https", "dynamodb." + region + "." + partitionResult.dnsSuffix(),
                                            -1, "")).build());
        }

        private RuleResult result11() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "dynamodb-fips." + region + "." + partitionResult.dnsSuffix(),
                                    -1, "")).build());
        }

        private RuleResult result12() {
            return RuleResult.error("FIPS is enabled but this partition does not support FIPS");
        }

        private RuleResult result13() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", parsedArn_ssa_2.accountId() + ".ddb." + region + "."
                                    + partitionResult.dualStackDnsSuffix(), -1, ""))
                    .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
        }

        private RuleResult result14() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", parsedArn_ssa_1.accountId() + ".ddb." + region + "."
                                    + partitionResult.dualStackDnsSuffix(), -1, ""))
                    .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
        }

        private RuleResult result15() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    params.accountId() + ".ddb." + region + "." + partitionResult.dualStackDnsSuffix(), -1, ""))
                    .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
        }

        private RuleResult result16() {
            return RuleResult.error("Credentials-sourced account ID parameter is invalid");
        }

        private RuleResult result17() {
            return RuleResult.error("AccountIdEndpointMode is required but no AccountID was provided or able to be loaded");
        }

        private RuleResult result18() {
            return RuleResult
                    .error("Invalid Configuration: AccountIdEndpointMode is required but account endpoints are not supported in this partition");
        }

        private RuleResult result19() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    "dynamodb." + region + "." + partitionResult.dualStackDnsSuffix(), -1, "")).build());
        }

        private RuleResult result20() {
            return RuleResult.error("DualStack is enabled but this partition does not support DualStack");
        }

        private RuleResult result21() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", parsedArn_ssa_2.accountId() + ".ddb." + region + "."
                                    + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
        }

        private RuleResult result22() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", parsedArn_ssa_1.accountId() + ".ddb." + region + "."
                                    + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
        }

        private RuleResult result23() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    params.accountId() + ".ddb." + region + "." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
        }

        private RuleResult result24() {
            return RuleResult.error("Invalid Configuration: Missing Region");
        }
    }
}
