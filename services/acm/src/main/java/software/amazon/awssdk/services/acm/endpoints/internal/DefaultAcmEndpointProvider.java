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

package software.amazon.awssdk.services.acm.endpoints.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.services.acm.endpoints.AcmEndpointParams;
import software.amazon.awssdk.services.acm.endpoints.AcmEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultAcmEndpointProvider implements AcmEndpointProvider {
    private static final EndpointRuleset ENDPOINT_RULE_SET = ruleSet();

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(AcmEndpointParams endpointParams) {
        Validate.notNull(endpointParams.useDualStack(), "Parameter 'UseDualStack' must not be null");
        Validate.notNull(endpointParams.useFips(), "Parameter 'UseFIPS' must not be null");
        Value res = new DefaultRuleEngine().evaluate(ENDPOINT_RULE_SET, toIdentifierValueMap(endpointParams));
        try {
            return CompletableFuture.completedFuture(AwsEndpointProviderUtils.valueAsEndpointOrThrow(res));
        } catch (Exception error) {
            return CompletableFutureUtils.failedFuture(error);
        }
    }

    private static Map<Identifier, Value> toIdentifierValueMap(AcmEndpointParams params) {
        Map<Identifier, Value> paramsMap = new HashMap<>();
        if (params.region() != null) {
            paramsMap.put(Identifier.of("Region"), Value.fromStr(params.region().id()));
        }
        if (params.useDualStack() != null) {
            paramsMap.put(Identifier.of("UseDualStack"), Value.fromBool(params.useDualStack()));
        }
        if (params.useFips() != null) {
            paramsMap.put(Identifier.of("UseFIPS"), Value.fromBool(params.useFips()));
        }
        if (params.endpoint() != null) {
            paramsMap.put(Identifier.of("Endpoint"), Value.fromStr(params.endpoint()));
        }
        return paramsMap;
    }

    private static Rule endpointRule_2() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("booleanEquals")
                                        .argv(Arrays.asList(Expr.ref(Identifier.of("UseFIPS")), Expr.of(true))).build()
                                        .validate()).build())
                .error("Invalid Configuration: FIPS and custom endpoint are not supported");
    }

    private static Rule endpointRule_4() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("booleanEquals")
                                        .argv(Arrays.asList(Expr.ref(Identifier.of("UseDualStack")), Expr.of(true))).build()
                                        .validate()).build())
                .error("Invalid Configuration: Dualstack and custom endpoint are not supported");
    }

    private static Rule endpointRule_5() {
        return Rule.builder().endpoint(EndpointResult.builder().url(Expr.ref(Identifier.of("Endpoint"))).build());
    }

    private static Rule endpointRule_3() {
        return Rule.builder().treeRule(Arrays.asList(endpointRule_4(), endpointRule_5()));
    }

    private static Rule endpointRule_1() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("isSet").argv(Arrays.asList(Expr.ref(Identifier.of("Endpoint")))).build()
                                        .validate()).build())
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("parseURL").argv(Arrays.asList(Expr.ref(Identifier.of("Endpoint"))))
                                        .build().validate()).result("url").build())
                .treeRule(Arrays.asList(endpointRule_2(), endpointRule_3()));
    }

    private static Rule endpointRule_8() {
        return Rule.builder().endpoint(
                EndpointResult.builder().url(Expr.of("https://acm-fips.{Region}.{PartitionResult#dualStackDnsSuffix}")).build());
    }

    private static Rule endpointRule_7() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode
                                        .builder()
                                        .fn("booleanEquals")
                                        .argv(Arrays.asList(
                                                Expr.of(true),
                                                FnNode.builder()
                                                        .fn("getAttr")
                                                        .argv(Arrays.asList(Expr.ref(Identifier.of("PartitionResult")),
                                                                Expr.of("supportsFIPS"))).build().validate())).build().validate())
                                .build())
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode
                                        .builder()
                                        .fn("booleanEquals")
                                        .argv(Arrays.asList(
                                                Expr.of(true),
                                                FnNode.builder()
                                                        .fn("getAttr")
                                                        .argv(Arrays.asList(Expr.ref(Identifier.of("PartitionResult")),
                                                                Expr.of("supportsDualStack"))).build().validate())).build()
                                        .validate()).build()).treeRule(Arrays.asList(endpointRule_8()));
    }

    private static Rule endpointRule_9() {
        return Rule.builder().error("FIPS and DualStack are enabled, but this partition does not support one or both");
    }

    private static Rule endpointRule_6() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("booleanEquals")
                                        .argv(Arrays.asList(Expr.ref(Identifier.of("UseFIPS")), Expr.of(true))).build()
                                        .validate()).build())
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("booleanEquals")
                                        .argv(Arrays.asList(Expr.ref(Identifier.of("UseDualStack")), Expr.of(true))).build()
                                        .validate()).build()).treeRule(Arrays.asList(endpointRule_7(), endpointRule_9()));
    }

    private static Rule endpointRule_13() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode
                                        .builder()
                                        .fn("stringEquals")
                                        .argv(Arrays.asList(
                                                Expr.of("aws-us-gov"),
                                                FnNode.builder()
                                                        .fn("getAttr")
                                                        .argv(Arrays.asList(Expr.ref(Identifier.of("PartitionResult")),
                                                                Expr.of("name"))).build().validate())).build().validate())
                                .build())
                .endpoint(EndpointResult.builder().url(Expr.of("https://acm.{Region}.{PartitionResult#dnsSuffix}")).build());
    }

    private static Rule endpointRule_14() {
        return Rule.builder().endpoint(
                EndpointResult.builder().url(Expr.of("https://acm-fips.{Region}.{PartitionResult#dnsSuffix}")).build());
    }

    private static Rule endpointRule_12() {
        return Rule.builder().treeRule(Arrays.asList(endpointRule_13(), endpointRule_14()));
    }

    private static Rule endpointRule_11() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode
                                        .builder()
                                        .fn("booleanEquals")
                                        .argv(Arrays.asList(
                                                Expr.of(true),
                                                FnNode.builder()
                                                        .fn("getAttr")
                                                        .argv(Arrays.asList(Expr.ref(Identifier.of("PartitionResult")),
                                                                Expr.of("supportsFIPS"))).build().validate())).build().validate())
                                .build()).treeRule(Arrays.asList(endpointRule_12()));
    }

    private static Rule endpointRule_15() {
        return Rule.builder().error("FIPS is enabled but this partition does not support FIPS");
    }

    private static Rule endpointRule_10() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("booleanEquals")
                                        .argv(Arrays.asList(Expr.ref(Identifier.of("UseFIPS")), Expr.of(true))).build()
                                        .validate()).build()).treeRule(Arrays.asList(endpointRule_11(), endpointRule_15()));
    }

    private static Rule endpointRule_18() {
        return Rule.builder().endpoint(
                EndpointResult.builder().url(Expr.of("https://acm.{Region}.{PartitionResult#dualStackDnsSuffix}")).build());
    }

    private static Rule endpointRule_17() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode
                                        .builder()
                                        .fn("booleanEquals")
                                        .argv(Arrays.asList(
                                                Expr.of(true),
                                                FnNode.builder()
                                                        .fn("getAttr")
                                                        .argv(Arrays.asList(Expr.ref(Identifier.of("PartitionResult")),
                                                                Expr.of("supportsDualStack"))).build().validate())).build()
                                        .validate()).build()).treeRule(Arrays.asList(endpointRule_18()));
    }

    private static Rule endpointRule_19() {
        return Rule.builder().error("DualStack is enabled but this partition does not support DualStack");
    }

    private static Rule endpointRule_16() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("booleanEquals")
                                        .argv(Arrays.asList(Expr.ref(Identifier.of("UseDualStack")), Expr.of(true))).build()
                                        .validate()).build()).treeRule(Arrays.asList(endpointRule_17(), endpointRule_19()));
    }

    private static Rule endpointRule_20() {
        return Rule.builder().endpoint(
                EndpointResult.builder().url(Expr.of("https://acm.{Region}.{PartitionResult#dnsSuffix}")).build());
    }

    private static Rule endpointRule_0() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("aws.partition").argv(Arrays.asList(Expr.ref(Identifier.of("Region"))))
                                        .build().validate()).result("PartitionResult").build())
                .treeRule(
                        Arrays.asList(endpointRule_1(), endpointRule_6(), endpointRule_10(), endpointRule_16(), endpointRule_20()));
    }

    private static EndpointRuleset ruleSet() {
        return EndpointRuleset
                .builder()
                .version("1.0")
                .serviceId(null)
                .parameters(
                        Parameters
                                .builder()
                                .addParameter(
                                        Parameter.builder().name("Region").type(ParameterType.fromValue("String"))
                                                .required(false).builtIn("AWS::Region")
                                                .documentation("The AWS region used to dispatch the request.").build())
                                .addParameter(
                                        Parameter
                                                .builder()
                                                .name("UseDualStack")
                                                .type(ParameterType.fromValue("Boolean"))
                                                .required(true)
                                                .builtIn("AWS::UseDualStack")
                                                .documentation(
                                                        "When true, use the dual-stack endpoint. If the configured endpoint does not support dual-stack, dispatching the request MAY return an error.")
                                                .defaultValue(Value.fromBool(false)).build())
                                .addParameter(
                                        Parameter
                                                .builder()
                                                .name("UseFIPS")
                                                .type(ParameterType.fromValue("Boolean"))
                                                .required(true)
                                                .builtIn("AWS::UseFIPS")
                                                .documentation(
                                                        "When true, send this request to the FIPS-compliant regional endpoint. If the configured endpoint does not have a FIPS compliant endpoint, dispatching the request will return an error.")
                                                .defaultValue(Value.fromBool(false)).build())
                                .addParameter(
                                        Parameter.builder().name("Endpoint").type(ParameterType.fromValue("String"))
                                                .required(false).builtIn("SDK::Endpoint")
                                                .documentation("Override the endpoint used to send this request").build())
                                .build()).addRule(endpointRule_0()).build();
    }
}
