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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointParams;
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class BaselineRulesEndpointResolver implements DynamoDbEndpointProvider {
    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(DynamoDbEndpointParams params) {
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

    private static RuleResult endpointRule0(DynamoDbEndpointParams params, String region) {
        RuleResult result = endpointRule1(params, region);
        if (result.isResolved()) {
            return result;
        }
        result = endpointRule6(params);
        if (result.isResolved()) {
            return result;
        }
        result = endpointRule10(params, region);
        if (result.isResolved()) {
            return result;
        }
        return RuleResult.error("Invalid Configuration: Missing Region");
    }

    private static RuleResult endpointRule1(DynamoDbEndpointParams params, String region) {
        if (params.endpoint() != null && region != null) {
            RulePartition partitionResult = RulesFunctions.awsPartition(region);
            if (partitionResult != null) {
                if (params.useFips()) {
                    return RuleResult.error("Invalid Configuration: FIPS and custom endpoint are not supported");
                }
                if (params.useDualStack()) {
                    return RuleResult.error("Invalid Configuration: Dualstack and custom endpoint are not supported");
                }
                if (RulesFunctions.stringEquals(params.endpoint(),
                                                "https://dynamodb." + region + "." + partitionResult.dualStackDnsSuffix())) {
                    return RuleResult
                        .error("Endpoint override is not supported for dual-stack endpoints. Please enable dual-stack functionality by enabling the configuration. For more details, see: https://docs.aws.amazon.com/sdkref/latest/guide/feature-endpoints.html");
                }
                return RuleResult.endpoint(Endpoint.builder().endpointUrl(EndpointUrl.fromString(params.endpoint())).build());
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule6(DynamoDbEndpointParams params) {
        if (params.endpoint() != null) {
            if (params.useFips()) {
                return RuleResult.error("Invalid Configuration: FIPS and custom endpoint are not supported");
            }
            if (params.useDualStack()) {
                return RuleResult.error("Invalid Configuration: Dualstack and custom endpoint are not supported");
            }
            return RuleResult.endpoint(Endpoint.builder().endpointUrl(EndpointUrl.fromString(params.endpoint())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule10(DynamoDbEndpointParams params, String region) {
        if (region != null) {
            return endpointRule11(params, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule11(DynamoDbEndpointParams params, String region) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            RuleResult result = endpointRule12(params, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule16(params, partitionResult, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule22(params, partitionResult, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule32(params, partitionResult, region);
            if (result.isResolved()) {
                return result;
            }
            if (params.accountIdEndpointMode() != null && !("disabled".equals(params.accountIdEndpointMode()))
                && "aws".equals(partitionResult.name()) && !(params.useFips()) && params.resourceArn() != null) {
                RuleArn parsedArn = RulesFunctions.awsParseArn(params.resourceArn());
                if (parsedArn != null) {
                    if ("dynamodb".equals(parsedArn.service()) && RulesFunctions.isValidHostLabel(parsedArn.region(), false)
                        && RulesFunctions.stringEquals(parsedArn.region(), region)
                        && RulesFunctions.isValidHostLabel(parsedArn.accountId(), false)) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .endpointUrl(
                                                           EndpointUrl.fromComponents("https", parsedArn.accountId() + ".ddb." + region + "."
                                                                                               + partitionResult.dnsSuffix(), -1, ""))
                                                       .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
                    }
                }
            }
            if (params.accountIdEndpointMode() != null && !("disabled".equals(params.accountIdEndpointMode()))
                && "aws".equals(partitionResult.name()) && !(params.useFips()) && params.resourceArnList() != null) {
                String firstArn = RulesFunctions.listAccess(params.resourceArnList(), 0);
                if (firstArn != null) {
                    RuleArn parsedArn = RulesFunctions.awsParseArn(firstArn);
                    if (parsedArn != null) {
                        if ("dynamodb".equals(parsedArn.service()) && RulesFunctions.isValidHostLabel(parsedArn.region(), false)
                            && RulesFunctions.stringEquals(parsedArn.region(), region)
                            && RulesFunctions.isValidHostLabel(parsedArn.accountId(), false)) {
                            return RuleResult.endpoint(Endpoint
                                                           .builder()
                                                           .endpointUrl(
                                                               EndpointUrl.fromComponents("https", parsedArn.accountId() + ".ddb." + region + "."
                                                                                                   + partitionResult.dnsSuffix(), -1, ""))
                                                           .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
                        }
                    }
                }
            }
            result = endpointRule50(params, partitionResult, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule54(params, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .endpointUrl(
                                  EndpointUrl.fromComponents("https", "dynamodb." + region + "." + partitionResult.dnsSuffix(),
                                                             -1, "")).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule12(DynamoDbEndpointParams params, String region) {
        if ("local".equals(region)) {
            if (params.useFips()) {
                return RuleResult.error("Invalid Configuration: FIPS and local endpoint are not supported");
            }
            if (params.useDualStack()) {
                return RuleResult.error("Invalid Configuration: Dualstack and local endpoint are not supported");
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .endpointUrl(EndpointUrl.fromComponents("http", "localhost", 8000, ""))
                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                                                         Arrays.asList(SigV4AuthScheme.builder().signingName("dynamodb").signingRegion("us-east-1").build()))
                                           .build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule16(DynamoDbEndpointParams params, RulePartition partitionResult, String region) {
        if (params.useFips() && params.useDualStack()) {
            RuleResult result = endpointRule17(params, partitionResult, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule17(DynamoDbEndpointParams params, RulePartition partitionResult, String region) {
        if (partitionResult.supportsFIPS() && partitionResult.supportsDualStack()) {
            RuleResult result = endpointRule18(params);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .endpointUrl(
                                               EndpointUrl.fromComponents("https",
                                                                          "dynamodb-fips." + region + "." + partitionResult.dualStackDnsSuffix(), -1, "")).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule18(DynamoDbEndpointParams params) {
        if (params.accountIdEndpointMode() != null && "required".equals(params.accountIdEndpointMode())) {
            return RuleResult
                .error("Invalid Configuration: AccountIdEndpointMode is required and FIPS is enabled, but FIPS account endpoints are not supported");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule22(DynamoDbEndpointParams params, RulePartition partitionResult, String region) {
        if (params.useFips()) {
            RuleResult result = endpointRule23(params, partitionResult, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("FIPS is enabled but this partition does not support FIPS");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule23(DynamoDbEndpointParams params, RulePartition partitionResult, String region) {
        if (partitionResult.supportsFIPS()) {
            RuleResult result = endpointRule24(params, partitionResult, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule28(params);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .endpointUrl(
                                               EndpointUrl.fromComponents("https", "dynamodb-fips." + region + "." + partitionResult.dnsSuffix(),
                                                                          -1, "")).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule24(DynamoDbEndpointParams params, RulePartition partitionResult, String region) {
        if ("aws-us-gov".equals(partitionResult.name())) {
            RuleResult result = endpointRule25(params);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .endpointUrl(
                                  EndpointUrl.fromComponents("https", "dynamodb." + region + "." + partitionResult.dnsSuffix(),
                                                             -1, "")).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule25(DynamoDbEndpointParams params) {
        if (params.accountIdEndpointMode() != null && "required".equals(params.accountIdEndpointMode())) {
            return RuleResult
                .error("Invalid Configuration: AccountIdEndpointMode is required and FIPS is enabled, but FIPS account endpoints are not supported");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule28(DynamoDbEndpointParams params) {
        if (params.accountIdEndpointMode() != null && "required".equals(params.accountIdEndpointMode())) {
            return RuleResult
                .error("Invalid Configuration: AccountIdEndpointMode is required and FIPS is enabled, but FIPS account endpoints are not supported");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule32(DynamoDbEndpointParams params, RulePartition partitionResult, String region) {
        if (params.useDualStack()) {
            RuleResult result = endpointRule33(params, partitionResult, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("DualStack is enabled but this partition does not support DualStack");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule33(DynamoDbEndpointParams params, RulePartition partitionResult, String region) {
        if (partitionResult.supportsDualStack()) {
            if (params.accountIdEndpointMode() != null && !("disabled".equals(params.accountIdEndpointMode()))
                && "aws".equals(partitionResult.name()) && !(params.useFips()) && params.resourceArn() != null) {
                RuleArn parsedArn = RulesFunctions.awsParseArn(params.resourceArn());
                if (parsedArn != null) {
                    if ("dynamodb".equals(parsedArn.service()) && RulesFunctions.isValidHostLabel(parsedArn.region(), false)
                        && RulesFunctions.stringEquals(parsedArn.region(), region)
                        && RulesFunctions.isValidHostLabel(parsedArn.accountId(), false)) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .endpointUrl(
                                                           EndpointUrl.fromComponents("https", parsedArn.accountId() + ".ddb." + region + "."
                                                                                               + partitionResult.dualStackDnsSuffix(), -1, ""))
                                                       .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
                    }
                }
            }
            if (params.accountIdEndpointMode() != null && !("disabled".equals(params.accountIdEndpointMode()))
                && "aws".equals(partitionResult.name()) && !(params.useFips()) && params.resourceArnList() != null) {
                String firstArn = RulesFunctions.listAccess(params.resourceArnList(), 0);
                if (firstArn != null) {
                    RuleArn parsedArn = RulesFunctions.awsParseArn(firstArn);
                    if (parsedArn != null) {
                        if ("dynamodb".equals(parsedArn.service()) && RulesFunctions.isValidHostLabel(parsedArn.region(), false)
                            && RulesFunctions.stringEquals(parsedArn.region(), region)
                            && RulesFunctions.isValidHostLabel(parsedArn.accountId(), false)) {
                            return RuleResult.endpoint(Endpoint
                                                           .builder()
                                                           .endpointUrl(
                                                               EndpointUrl.fromComponents("https", parsedArn.accountId() + ".ddb." + region + "."
                                                                                                   + partitionResult.dualStackDnsSuffix(), -1, ""))
                                                           .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
                        }
                    }
                }
            }
            RuleResult result = endpointRule36(params, partitionResult, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule40(params, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .endpointUrl(
                                               EndpointUrl.fromComponents("https",
                                                                          "dynamodb." + region + "." + partitionResult.dualStackDnsSuffix(), -1, "")).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule36(DynamoDbEndpointParams params, RulePartition partitionResult, String region) {
        if (params.accountIdEndpointMode() != null && !("disabled".equals(params.accountIdEndpointMode()))
            && "aws".equals(partitionResult.name()) && !(params.useFips()) && params.accountId() != null) {
            RuleResult result = endpointRule37(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Credentials-sourced account ID parameter is invalid");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule37(DynamoDbEndpointParams params, String region, RulePartition partitionResult) {
        if (RulesFunctions.isValidHostLabel(params.accountId(), false)) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .endpointUrl(
                                               EndpointUrl.fromComponents("https",
                                                                          params.accountId() + ".ddb." + region + "." + partitionResult.dualStackDnsSuffix(), -1, ""))
                                           .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule40(DynamoDbEndpointParams params, RulePartition partitionResult) {
        if (params.accountIdEndpointMode() != null && "required".equals(params.accountIdEndpointMode())) {
            RuleResult result = endpointRule41(params, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult
                .error("Invalid Configuration: AccountIdEndpointMode is required and FIPS is enabled, but FIPS account endpoints are not supported");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule41(DynamoDbEndpointParams params, RulePartition partitionResult) {
        if (!(params.useFips())) {
            RuleResult result = endpointRule42(params, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult
                .error("Invalid Configuration: AccountIdEndpointMode is required but account endpoints are not supported in this partition");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule42(DynamoDbEndpointParams params, RulePartition partitionResult) {
        if ("aws".equals(partitionResult.name())) {
            return RuleResult.error("AccountIdEndpointMode is required but no AccountID was provided or able to be loaded");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule50(DynamoDbEndpointParams params, RulePartition partitionResult, String region) {
        if (params.accountIdEndpointMode() != null && !("disabled".equals(params.accountIdEndpointMode()))
            && "aws".equals(partitionResult.name()) && !(params.useFips()) && params.accountId() != null) {
            RuleResult result = endpointRule51(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Credentials-sourced account ID parameter is invalid");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule51(DynamoDbEndpointParams params, String region, RulePartition partitionResult) {
        if (RulesFunctions.isValidHostLabel(params.accountId(), false)) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .endpointUrl(
                                               EndpointUrl.fromComponents("https",
                                                                          params.accountId() + ".ddb." + region + "." + partitionResult.dnsSuffix(), -1, ""))
                                           .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("O")).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule54(DynamoDbEndpointParams params, RulePartition partitionResult) {
        if (params.accountIdEndpointMode() != null && "required".equals(params.accountIdEndpointMode())) {
            RuleResult result = endpointRule55(params, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult
                .error("Invalid Configuration: AccountIdEndpointMode is required and FIPS is enabled, but FIPS account endpoints are not supported");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule55(DynamoDbEndpointParams params, RulePartition partitionResult) {
        if (!(params.useFips())) {
            RuleResult result = endpointRule56(params, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult
                .error("Invalid Configuration: AccountIdEndpointMode is required but account endpoints are not supported in this partition");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule56(DynamoDbEndpointParams params, RulePartition partitionResult) {
        if ("aws".equals(partitionResult.name())) {
            return RuleResult.error("AccountIdEndpointMode is required but no AccountID was provided or able to be loaded");
        }
        return RuleResult.carryOn();
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
