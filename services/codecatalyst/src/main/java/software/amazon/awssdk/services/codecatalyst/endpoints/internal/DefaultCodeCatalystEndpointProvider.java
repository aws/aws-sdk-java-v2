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

package software.amazon.awssdk.services.codecatalyst.endpoints.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.services.codecatalyst.endpoints.CodeCatalystEndpointParams;
import software.amazon.awssdk.services.codecatalyst.endpoints.CodeCatalystEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultCodeCatalystEndpointProvider implements CodeCatalystEndpointProvider {
    private static final EndpointRuleset ENDPOINT_RULE_SET = ruleSet();

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(CodeCatalystEndpointParams endpointParams) {
        Validate.notNull(endpointParams.useFips(), "Parameter 'UseFIPS' must not be null");
        Value res = new DefaultRuleEngine().evaluate(ENDPOINT_RULE_SET, toIdentifierValueMap(endpointParams));
        try {
            return CompletableFuture.completedFuture(AwsEndpointProviderUtils.valueAsEndpointOrThrow(res));
        } catch (Exception error) {
            return CompletableFutureUtils.failedFuture(error);
        }
    }

    private static Map<Identifier, Value> toIdentifierValueMap(CodeCatalystEndpointParams params) {
        Map<Identifier, Value> paramsMap = new HashMap<>();
        if (params.useFips() != null) {
            paramsMap.put(Identifier.of("UseFIPS"), Value.fromBool(params.useFips()));
        }
        if (params.region() != null) {
            paramsMap.put(Identifier.of("Region"), Value.fromStr(params.region().id()));
        }
        if (params.endpoint() != null) {
            paramsMap.put(Identifier.of("Endpoint"), Value.fromStr(params.endpoint()));
        }
        return paramsMap;
    }

    private static Rule endpointRule_1() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("isSet").argv(Arrays.asList(Expr.ref(Identifier.of("Endpoint")))).build()
                                        .validate()).build())
                .endpoint(EndpointResult.builder().url(Expr.ref(Identifier.of("Endpoint"))).build());
    }

    private static Rule endpointRule_4() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode
                                        .builder()
                                        .fn("booleanEquals")
                                        .argv(Arrays.asList(
                                                FnNode.builder()
                                                        .fn("getAttr")
                                                        .argv(Arrays.asList(Expr.ref(Identifier.of("PartitionResult")),
                                                                Expr.of("supportsFIPS"))).build().validate(), Expr.of(false)))
                                        .build().validate()).build()).error("Partition does not support FIPS.");
    }

    private static Rule endpointRule_5() {
        return Rule.builder().endpoint(
                EndpointResult.builder().url(Expr.of("https://codecatalyst-fips.global.{PartitionResult#dualStackDnsSuffix}"))
                        .build());
    }

    private static Rule endpointRule_3() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("booleanEquals")
                                        .argv(Arrays.asList(Expr.ref(Identifier.of("UseFIPS")), Expr.of(true))).build()
                                        .validate()).build()).treeRule(Arrays.asList(endpointRule_4(), endpointRule_5()));
    }

    private static Rule endpointRule_6() {
        return Rule.builder()
                .endpoint(
                        EndpointResult.builder().url(Expr.of("https://codecatalyst.global.{PartitionResult#dualStackDnsSuffix}"))
                                .build());
    }

    private static Rule endpointRule_2() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode
                                        .builder()
                                        .fn("not")
                                        .argv(Arrays.asList(FnNode.builder().fn("isSet")
                                                .argv(Arrays.asList(Expr.ref(Identifier.of("Region")))).build().validate()))
                                        .build().validate()).build())
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("aws.partition").argv(Arrays.asList(Expr.of("us-west-2"))).build()
                                        .validate()).result("PartitionResult").build())
                .treeRule(Arrays.asList(endpointRule_3(), endpointRule_6()));
    }

    private static Rule endpointRule_9() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode
                                        .builder()
                                        .fn("booleanEquals")
                                        .argv(Arrays.asList(
                                                FnNode.builder()
                                                        .fn("getAttr")
                                                        .argv(Arrays.asList(Expr.ref(Identifier.of("PartitionResult")),
                                                                Expr.of("supportsFIPS"))).build().validate(), Expr.of(false)))
                                        .build().validate()).build()).error("Partition does not support FIPS.");
    }

    private static Rule endpointRule_10() {
        return Rule.builder().endpoint(
                EndpointResult.builder().url(Expr.of("https://codecatalyst-fips.global.{PartitionResult#dualStackDnsSuffix}"))
                        .build());
    }

    private static Rule endpointRule_8() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("booleanEquals")
                                        .argv(Arrays.asList(Expr.ref(Identifier.of("UseFIPS")), Expr.of(true))).build()
                                        .validate()).build()).treeRule(Arrays.asList(endpointRule_9(), endpointRule_10()));
    }

    private static Rule endpointRule_11() {
        return Rule.builder()
                .endpoint(
                        EndpointResult.builder().url(Expr.of("https://codecatalyst.global.{PartitionResult#dualStackDnsSuffix}"))
                                .build());
    }

    private static Rule endpointRule_7() {
        return Rule
                .builder()
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("isSet").argv(Arrays.asList(Expr.ref(Identifier.of("Region")))).build()
                                        .validate()).build())
                .addCondition(
                        Condition
                                .builder()
                                .fn(FnNode.builder().fn("aws.partition").argv(Arrays.asList(Expr.ref(Identifier.of("Region"))))
                                        .build().validate()).result("PartitionResult").build())
                .treeRule(Arrays.asList(endpointRule_8(), endpointRule_11()));
    }

    private static Rule endpointRule_0() {
        return Rule.builder().treeRule(Arrays.asList(endpointRule_1(), endpointRule_2(), endpointRule_7()));
    }

    private static EndpointRuleset ruleSet() {
        return EndpointRuleset
                .builder()
                .version("1.3")
                .serviceId(null)
                .parameters(
                        Parameters
                                .builder()
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
                                        Parameter.builder().name("Region").type(ParameterType.fromValue("String"))
                                                .required(false).builtIn("AWS::Region")
                                                .documentation("The AWS region used to dispatch the request.").build())
                                .addParameter(
                                        Parameter.builder().name("Endpoint").type(ParameterType.fromValue("String"))
                                                .required(false).builtIn("SDK::Endpoint")
                                                .documentation("Override the endpoint used to send this request").build())
                                .build()).addRule(endpointRule_0()).build();
    }
}
