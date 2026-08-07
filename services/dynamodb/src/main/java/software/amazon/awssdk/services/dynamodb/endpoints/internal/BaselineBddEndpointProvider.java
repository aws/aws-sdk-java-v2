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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
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
public final class BaselineBddEndpointProvider implements DynamoDbEndpointProvider {
    private static final int[] BDD_DEFINITION;

    static {
        try (InputStream in = DefaultDynamoDbEndpointProvider.class.getResourceAsStream("/endpoints_bdd_afbb224b.bin")) {
            if (in == null) {
                throw new IllegalStateException("Resource /endpoints_bdd_afbb224b.bin not found");
            }
            BDD_DEFINITION = new int[201];
            DataInputStream data = new DataInputStream(in);
            for (int i = 0; i < 201; i++) {
                BDD_DEFINITION[i] = data.readInt();
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(DynamoDbEndpointParams params) {
        Evaluator evaluator = new Evaluator();
        evaluator.region = params.region() == null ? null : params.region().id();
        evaluator.params = params;
        final int[] bdd = BDD_DEFINITION;
        int nodeRef = 2;
        while ((nodeRef > 1 || nodeRef < -1) && nodeRef < 100000000) {
            int base = (Math.abs(nodeRef) - 1) * 3;
            int complemented = nodeRef >> 31 & 1; // 1 if complemented edge, else 0;
            int conditionResult = evaluator.cond(bdd[base]) ? 1 : 0;
            nodeRef = bdd[base + 2 - (complemented ^ conditionResult)];
        }
        if (nodeRef == 1 || nodeRef == -1) {
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
        DynamoDbEndpointParams params;

        String region;

        RulePartition partitionResult;

        RuleArn parsedArn_ssa_2;

        String firstArn;

        RuleArn parsedArn_ssa_1;

        public final boolean cond(int i) {
            switch (i) {
            case 0: {
                return (region != null);
            }
            case 1: {
                return (params.endpoint() != null);
            }
            case 2: {
                return (params.useFips());
            }
            case 3: {
                partitionResult = RulesFunctions.awsPartition(region);
                return partitionResult != null;
            }
            case 4: {
                return ("local".equals(region));
            }
            case 5: {
                return (partitionResult.supportsFIPS());
            }
            case 6: {
                return (params.useDualStack());
            }
            case 7: {
                return (partitionResult.supportsDualStack());
            }
            case 8: {
                return (RulesFunctions.stringEquals("https://dynamodb." + region + "." + partitionResult.dualStackDnsSuffix(),
                        params.endpoint()));
            }
            case 9: {
                return (params.accountIdEndpointMode() != null);
            }
            case 10: {
                return ("aws".equals(partitionResult.name()));
            }
            case 11: {
                return ("disabled".equals(params.accountIdEndpointMode()));
            }
            case 12: {
                return (params.resourceArn() != null);
            }
            case 13: {
                parsedArn_ssa_2 = RulesFunctions.awsParseArn(params.resourceArn());
                return parsedArn_ssa_2 != null;
            }
            case 14: {
                return (RulesFunctions.stringEquals(parsedArn_ssa_2.region(), region));
            }
            case 15: {
                return ("dynamodb".equals(parsedArn_ssa_2.service()));
            }
            case 16: {
                return (RulesFunctions.isValidHostLabel(parsedArn_ssa_2.accountId(), false));
            }
            case 17: {
                return (RulesFunctions.isValidHostLabel(parsedArn_ssa_2.region(), false));
            }
            case 18: {
                return (params.resourceArnList() != null);
            }
            case 19: {
                firstArn = RulesFunctions.listAccess(params.resourceArnList(), 0);
                return firstArn != null;
            }
            case 20: {
                parsedArn_ssa_1 = RulesFunctions.awsParseArn(firstArn);
                return parsedArn_ssa_1 != null;
            }
            case 21: {
                return (RulesFunctions.stringEquals(parsedArn_ssa_1.region(), region));
            }
            case 22: {
                return ("dynamodb".equals(parsedArn_ssa_1.service()));
            }
            case 23: {
                return (RulesFunctions.isValidHostLabel(parsedArn_ssa_1.accountId(), false));
            }
            case 24: {
                return (RulesFunctions.isValidHostLabel(parsedArn_ssa_1.region(), false));
            }
            case 25: {
                return (params.accountId() != null);
            }
            case 26: {
                return ("required".equals(params.accountIdEndpointMode()));
            }
            case 27: {
                return (RulesFunctions.isValidHostLabel(params.accountId(), false));
            }
            case 28: {
                return ("aws-us-gov".equals(partitionResult.name()));
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
            case 12: {
                return result12();
            }
            case 13: {
                return result13();
            }
            case 14: {
                return result14();
            }
            case 15: {
                return result15();
            }
            case 16: {
                return result16();
            }
            case 17: {
                return result17();
            }
            case 18: {
                return result18();
            }
            case 19: {
                return result19();
            }
            case 20: {
                return result20();
            }
            case 21: {
                return result21();
            }
            case 22: {
                return result22();
            }
            case 23: {
                return result23();
            }
            case 24: {
                return result24();
            }
            default: {
                throw new IllegalArgumentException("Unknown result index");
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
            return RuleResult
                    .error("Endpoint override is not supported for dual-stack endpoints. Please enable dual-stack functionality by enabling the configuration. For more details, see: https://docs.aws.amazon.com/sdkref/latest/guide/feature-endpoints.html");
        }

        private final RuleResult result3() {
            return RuleResult.endpoint(Endpoint.builder().endpointUrl(EndpointUrl.fromString(params.endpoint())).build());
        }

        private final RuleResult result4() {
            return RuleResult.error("Invalid Configuration: FIPS and local endpoint are not supported");
        }

        private final RuleResult result5() {
            return RuleResult.error("Invalid Configuration: Dualstack and local endpoint are not supported");
        }

        private final RuleResult result6() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(EndpointUrl.fromComponents("http", "localhost", 8000, ""))
                    .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().signingName("dynamodb").signingRegion("us-east-1").build()))
                    .build());
        }

        private final RuleResult result7() {
            return RuleResult
                    .error("Invalid Configuration: AccountIdEndpointMode is required and FIPS is enabled, but FIPS account endpoints are not supported");
        }

        private final RuleResult result8() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    "dynamodb-fips." + region + "." + partitionResult.dualStackDnsSuffix(), -1, "")).build());
        }

        private final RuleResult result9() {
            return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
        }

        private final RuleResult result10() {
            return RuleResult
                    .endpoint(Endpoint
                            .builder()
                            .endpointUrl(
                                    EndpointUrl.fromComponents("https", "dynamodb." + region + "." + partitionResult.dnsSuffix(),
                                            -1, "")).build());
        }

        private final RuleResult result11() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "dynamodb-fips." + region + "." + partitionResult.dnsSuffix(),
                                    -1, "")).build());
        }

        private final RuleResult result12() {
            return RuleResult.error("FIPS is enabled but this partition does not support FIPS");
        }

        private final RuleResult result13() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", parsedArn_ssa_2.accountId() + ".ddb." + region + "."
                                    + partitionResult.dualStackDnsSuffix(), -1, ""))
                    .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
        }

        private final RuleResult result14() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", parsedArn_ssa_1.accountId() + ".ddb." + region + "."
                                    + partitionResult.dualStackDnsSuffix(), -1, ""))
                    .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
        }

        private final RuleResult result15() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    params.accountId() + ".ddb." + region + "." + partitionResult.dualStackDnsSuffix(), -1, ""))
                    .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
        }

        private final RuleResult result16() {
            return RuleResult.error("Credentials-sourced account ID parameter is invalid");
        }

        private final RuleResult result17() {
            return RuleResult.error("AccountIdEndpointMode is required but no AccountID was provided or able to be loaded");
        }

        private final RuleResult result18() {
            return RuleResult
                    .error("Invalid Configuration: AccountIdEndpointMode is required but account endpoints are not supported in this partition");
        }

        private final RuleResult result19() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    "dynamodb." + region + "." + partitionResult.dualStackDnsSuffix(), -1, "")).build());
        }

        private final RuleResult result20() {
            return RuleResult.error("DualStack is enabled but this partition does not support DualStack");
        }

        private final RuleResult result21() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", parsedArn_ssa_2.accountId() + ".ddb." + region + "."
                                    + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
        }

        private final RuleResult result22() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", parsedArn_ssa_1.accountId() + ".ddb." + region + "."
                                    + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
        }

        private final RuleResult result23() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    params.accountId() + ".ddb." + region + "." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
        }

        private final RuleResult result24() {
            return RuleResult.error("Invalid Configuration: Missing Region");
        }
    }
}
