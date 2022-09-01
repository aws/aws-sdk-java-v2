package software.amazon.awssdk.services.query.rules.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.rules.Condition;
import software.amazon.awssdk.core.rules.DefaultRuleEngine;
import software.amazon.awssdk.core.rules.EndpointRuleset;
import software.amazon.awssdk.core.rules.Expr;
import software.amazon.awssdk.core.rules.FnNode;
import software.amazon.awssdk.core.rules.Identifier;
import software.amazon.awssdk.core.rules.Parameter;
import software.amazon.awssdk.core.rules.ParameterType;
import software.amazon.awssdk.core.rules.Parameters;
import software.amazon.awssdk.core.rules.ProviderUtils;
import software.amazon.awssdk.core.rules.Rule;
import software.amazon.awssdk.core.rules.RuleEngine;
import software.amazon.awssdk.core.rules.Value;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.services.query.rules.QueryEndpointParameters;
import software.amazon.awssdk.services.query.rules.QueryEndpointProvider;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryEndpointProvider implements QueryEndpointProvider {
    private static final RuleEngine RULES_ENGINE = new DefaultRuleEngine();

    private static final EndpointRuleset ENDPOINT_RULE_SET = ruleSet();

    @Override
    public Endpoint resolveEndpoint(QueryEndpointParameters endpointParams) {
        Value res = RULES_ENGINE.evaluate(ENDPOINT_RULE_SET, toIdentifierValueMap(endpointParams));
        return ProviderUtils.fromEndpointValue(res.expectEndpoint());
    }

    private static Map<Identifier, Value> toIdentifierValueMap(QueryEndpointParameters params) {
        Map<Identifier, Value> paramsMap = new HashMap<>();
        String region = params.region().id();
        if (region != null) {
            paramsMap.put(Identifier.of("region"), Value.fromStr(region));
        }
        Boolean useDualStackEndpoint = params.useDualStackEndpoint();
        if (useDualStackEndpoint != null) {
            paramsMap.put(Identifier.of("useDualStackEndpoint"), Value.fromBool(useDualStackEndpoint));
        }
        Boolean useFIPSEndpoint = params.useFIPSEndpoint();
        if (useFIPSEndpoint != null) {
            paramsMap.put(Identifier.of("useFIPSEndpoint"), Value.fromBool(useFIPSEndpoint));
        }
        String endpointId = params.endpointId();
        if (endpointId != null) {
            paramsMap.put(Identifier.of("endpointId"), Value.fromStr(endpointId));
        }
        Boolean defaultTrueParam = params.defaultTrueParam();
        if (defaultTrueParam != null) {
            paramsMap.put(Identifier.of("defaultTrueParam"), Value.fromBool(defaultTrueParam));
        }
        String defaultStringParam = params.defaultStringParam();
        if (defaultStringParam != null) {
            paramsMap.put(Identifier.of("defaultStringParam"), Value.fromStr(defaultStringParam));
        }
        String deprecatedParam = params.deprecatedParam();
        if (deprecatedParam != null) {
            paramsMap.put(Identifier.of("deprecatedParam"), Value.fromStr(deprecatedParam));
        }
        return paramsMap;
    }

    private static EndpointRuleset ruleSet() {
        return EndpointRuleset
            .builder()
            .version("1.2")
            .serviceId("query")
            .parameters(
                Parameters
                    .builder()
                    .addParameter(
                        Parameter.builder().name("region").type(ParameterType.fromValue("string")).required(true)
                                 .builtIn("AWS::Region").documentation("The region to send requests to").build())
                    .addParameter(
                        Parameter.builder().name("useDualStackEndpoint").type(ParameterType.fromValue("boolean"))
                                 .required(false).builtIn("AWS::UseDualStackEndpoint").build())
                    .addParameter(
                        Parameter.builder().name("useFIPSEndpoint").type(ParameterType.fromValue("boolean"))
                                 .required(false).builtIn("AWS::UseFIPSEndpoint").build())
                    .addParameter(
                        Parameter.builder().name("endpointId").type(ParameterType.fromValue("string"))
                                 .required(false).build())
                    .addParameter(
                        Parameter.builder().name("defaultTrueParam").type(ParameterType.fromValue("boolean"))
                                 .required(false).defaultValue(Value.fromBool(true)).build())
                    .addParameter(
                        Parameter.builder().name("defaultStringParam").type(ParameterType.fromValue("string"))
                                 .required(false).defaultValue(Value.fromStr("hello endpoints")).build())
                    .addParameter(
                        Parameter.builder().name("deprecatedParam").type(ParameterType.fromValue("string"))
                                 .required(false).deprecated(new Parameter.Deprecated("Don't use!", "2021-01-01"))
                                 .build()).build())
            .addRule(
                Rule.builder()
                    .addCondition(
                        Condition
                            .builder()
                            .fn(FnNode.builder().fn("partition")
                                      .argv(Arrays.asList(Expr.ref(Identifier.of("region")))).build()
                                      .validate()).result("partitionResult").build())
                    .treeRule(
                        Arrays.asList(
                            Rule.builder()
                                .addCondition(
                                    Condition
                                        .builder()
                                        .fn(FnNode
                                                .builder()
                                                .fn("isSet")
                                                .argv(Arrays.asList(Expr.ref(Identifier
                                                                                 .of("endpointId")))).build().validate())
                                        .build())
                                .treeRule(
                                    Arrays.asList(
                                        Rule.builder()
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("isSet")
                                                            .argv(Arrays.asList(Expr.ref(Identifier
                                                                                             .of("useFIPSEndpoint"))))
                                                            .build().validate())
                                                    .build())
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("booleanEquals")
                                                            .argv(Arrays.asList(
                                                                Expr.ref(Identifier
                                                                             .of("useFIPSEndpoint")),
                                                                Expr.of(true)))
                                                            .build().validate())
                                                    .build())
                                            .error("FIPS endpoints not supported with multi-region endpoints"),
                                        Rule.builder()
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("not")
                                                            .argv(Arrays
                                                                      .asList(FnNode
                                                                                  .builder()
                                                                                  .fn("isSet")
                                                                                  .argv(Arrays
                                                                                            .asList(Expr
                                                                                                        .ref(Identifier
                                                                                                                 .of("useFIPSEndpoint"))))
                                                                                  .build()
                                                                                  .validate()))
                                                            .build().validate())
                                                    .build())
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("isSet")
                                                            .argv(Arrays.asList(Expr.ref(Identifier
                                                                                             .of("useDualStackEndpoint"))))
                                                            .build().validate())
                                                    .build())
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("booleanEquals")
                                                            .argv(Arrays.asList(
                                                                Expr.ref(Identifier
                                                                             .of("useDualStackEndpoint")),
                                                                Expr.of(true)))
                                                            .build().validate())
                                                    .build())
                                            .endpoint(
                                                software.amazon.awssdk.core.rules.Endpoint
                                                    .builder()
                                                    .url(Expr
                                                             .of("https://{endpointId}.query.{partitionResult#dualStackDnsSuffix}"))
                                                    .build()),
                                        Rule.builder()
                                            .endpoint(
                                                software.amazon.awssdk.core.rules.Endpoint
                                                    .builder()
                                                    .url(Expr
                                                             .of("https://{endpointId}.query.{partitionResult#dnsSuffix}"))
                                                    .build()))),
                            Rule.builder()
                                .addCondition(
                                    Condition
                                        .builder()
                                        .fn(FnNode
                                                .builder()
                                                .fn("isValidHostLabel")
                                                .argv(Arrays.asList(
                                                    Expr.ref(Identifier.of("region")),
                                                    Expr.of(false))).build().validate())
                                        .build())
                                .treeRule(
                                    Arrays.asList(
                                        Rule.builder()
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("isSet")
                                                            .argv(Arrays.asList(Expr.ref(Identifier
                                                                                             .of("useFIPSEndpoint"))))
                                                            .build().validate())
                                                    .build())
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("booleanEquals")
                                                            .argv(Arrays.asList(
                                                                Expr.ref(Identifier
                                                                             .of("useFIPSEndpoint")),
                                                                Expr.of(true)))
                                                            .build().validate())
                                                    .build())
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("not")
                                                            .argv(Arrays
                                                                      .asList(FnNode
                                                                                  .builder()
                                                                                  .fn("isSet")
                                                                                  .argv(Arrays
                                                                                            .asList(Expr
                                                                                                        .ref(Identifier
                                                                                                                 .of("useDualStackEndpoint"))))
                                                                                  .build()
                                                                                  .validate()))
                                                            .build().validate())
                                                    .build())
                                            .endpoint(
                                                software.amazon.awssdk.core.rules.Endpoint
                                                    .builder()
                                                    .url(Expr
                                                             .of("https://query-fips.{region}.{partitionResult#dnsSuffix}"))
                                                    .build()),
                                        Rule.builder()
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("isSet")
                                                            .argv(Arrays.asList(Expr.ref(Identifier
                                                                                             .of("useDualStackEndpoint"))))
                                                            .build().validate())
                                                    .build())
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("booleanEquals")
                                                            .argv(Arrays.asList(
                                                                Expr.ref(Identifier
                                                                             .of("useDualStackEndpoint")),
                                                                Expr.of(true)))
                                                            .build().validate())
                                                    .build())
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("not")
                                                            .argv(Arrays
                                                                      .asList(FnNode
                                                                                  .builder()
                                                                                  .fn("isSet")
                                                                                  .argv(Arrays
                                                                                            .asList(Expr
                                                                                                        .ref(Identifier
                                                                                                                 .of("useFIPSEndpoint"))))
                                                                                  .build()
                                                                                  .validate()))
                                                            .build().validate())
                                                    .build())
                                            .endpoint(
                                                software.amazon.awssdk.core.rules.Endpoint
                                                    .builder()
                                                    .url(Expr
                                                             .of("https://query.{region}.{partitionResult#dualStackDnsSuffix}"))
                                                    .build()),
                                        Rule.builder()
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("isSet")
                                                            .argv(Arrays.asList(Expr.ref(Identifier
                                                                                             .of("useDualStackEndpoint"))))
                                                            .build().validate())
                                                    .build())
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("isSet")
                                                            .argv(Arrays.asList(Expr.ref(Identifier
                                                                                             .of("useFIPSEndpoint"))))
                                                            .build().validate())
                                                    .build())
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("booleanEquals")
                                                            .argv(Arrays.asList(
                                                                Expr.ref(Identifier
                                                                             .of("useDualStackEndpoint")),
                                                                Expr.of(true)))
                                                            .build().validate())
                                                    .build())
                                            .addCondition(
                                                Condition
                                                    .builder()
                                                    .fn(FnNode
                                                            .builder()
                                                            .fn("booleanEquals")
                                                            .argv(Arrays.asList(
                                                                Expr.ref(Identifier
                                                                             .of("useFIPSEndpoint")),
                                                                Expr.of(true)))
                                                            .build().validate())
                                                    .build())
                                            .endpoint(
                                                software.amazon.awssdk.core.rules.Endpoint
                                                    .builder()
                                                    .url(Expr
                                                             .of("https://query-fips.{region}.{partitionResult#dualStackDnsSuffix}"))
                                                    .build()),
                                        Rule.builder()
                                            .endpoint(
                                                software.amazon.awssdk.core.rules.Endpoint
                                                    .builder()
                                                    .url(Expr
                                                             .of("https://query.{region}.{partitionResult#dnsSuffix}"))
                                                    .build()))), Rule.builder()
                                                                     .error("{region} is not a valid HTTP host-label")))).build();
    }
}
