package software.amazon.awssdk.services.query.endpoints.internal;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointParams;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.MapUtils;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryEndpointProvider implements QueryEndpointProvider {
    private static final Logger LOG = Logger.loggerFor(DefaultQueryEndpointProvider.class);

    private static final EndpointRuleset ENDPOINT_RULE_SET = ruleSet();

    private final EndpointAuthSchemeStrategy endpointAuthSchemeStrategy;

    public DefaultQueryEndpointProvider() {
        EndpointAuthSchemeStrategyFactory endpointAuthSchemeStrategyFactory = new DefaultEndpointAuthSchemeStrategyFactory();
        this.endpointAuthSchemeStrategy = endpointAuthSchemeStrategyFactory.endpointAuthSchemeStrategy();
    }

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(QueryEndpointParams endpointParams) {
        Validate.notNull(endpointParams.region(), "Parameter 'region' must not be null");
        Value res = new DefaultRuleEngine().evaluate(ENDPOINT_RULE_SET, toIdentifierValueMap(endpointParams));
        try {
            return CompletableFuture.completedFuture(valueAsEndpointOrThrow(res));
        } catch (Exception error) {
            return CompletableFutureUtils.failedFuture(error);
        }
    }

    private static Map<Identifier, Value> toIdentifierValueMap(QueryEndpointParams params) {
        Map<Identifier, Value> paramsMap = new HashMap<>();
        if (params.region() != null) {
            paramsMap.put(Identifier.of("region"), Value.fromStr(params.region().id()));
        }
        if (params.useDualStackEndpoint() != null) {
            paramsMap.put(Identifier.of("useDualStackEndpoint"), Value.fromBool(params.useDualStackEndpoint()));
        }
        if (params.useFipsEndpoint() != null) {
            paramsMap.put(Identifier.of("useFIPSEndpoint"), Value.fromBool(params.useFipsEndpoint()));
        }
        if (params.accountId() != null) {
            paramsMap.put(Identifier.of("AccountId"), Value.fromStr(params.accountId()));
        }
        if (params.accountIdEndpointMode() != null) {
            paramsMap.put(Identifier.of("AccountIdEndpointMode"), Value.fromStr(params.accountIdEndpointMode()));
        }
        if (params.listOfStrings() != null) {
            paramsMap.put(Identifier.of("listOfStrings"),
                          Value.fromArray(params.listOfStrings().stream().map(Value::fromStr).collect(Collectors.toList())));
        }
        if (params.defaultListOfStrings() != null) {
            paramsMap.put(Identifier.of("defaultListOfStrings"),
                          Value.fromArray(params.defaultListOfStrings().stream().map(Value::fromStr).collect(Collectors.toList())));
        }
        if (params.endpointId() != null) {
            paramsMap.put(Identifier.of("endpointId"), Value.fromStr(params.endpointId()));
        }
        if (params.defaultTrueParam() != null) {
            paramsMap.put(Identifier.of("defaultTrueParam"), Value.fromBool(params.defaultTrueParam()));
        }
        if (params.defaultStringParam() != null) {
            paramsMap.put(Identifier.of("defaultStringParam"), Value.fromStr(params.defaultStringParam()));
        }
        if (params.deprecatedParam() != null) {
            paramsMap.put(Identifier.of("deprecatedParam"), Value.fromStr(params.deprecatedParam()));
        }
        if (params.booleanContextParam() != null) {
            paramsMap.put(Identifier.of("booleanContextParam"), Value.fromBool(params.booleanContextParam()));
        }
        if (params.stringContextParam() != null) {
            paramsMap.put(Identifier.of("stringContextParam"), Value.fromStr(params.stringContextParam()));
        }
        if (params.operationContextParam() != null) {
            paramsMap.put(Identifier.of("operationContextParam"), Value.fromStr(params.operationContextParam()));
        }
        if (params.customEndpointArray() != null) {
            paramsMap.put(Identifier.of("CustomEndpointArray"),
                          Value.fromArray(params.customEndpointArray().stream().map(Value::fromStr).collect(Collectors.toList())));
        }
        if (params.arnList() != null) {
            paramsMap.put(Identifier.of("ArnList"),
                          Value.fromArray(params.arnList().stream().map(Value::fromStr).collect(Collectors.toList())));
        }
        return paramsMap;
    }

    Endpoint valueAsEndpointOrThrow(Value value) {
        if (value instanceof Value.Endpoint) {
            Value.Endpoint endpoint = value.expectEndpoint();
            Endpoint.Builder builder = Endpoint.builder();
            builder.url(URI.create(endpoint.getUrl()));
            Map<String, List<String>> headers = endpoint.getHeaders();
            if (headers != null) {
                headers.forEach((name, values) -> values.forEach(v -> builder.putHeader(name, v)));
            }
            addKnownProperties(builder, endpoint.getProperties());
            return builder.build();
        } else if (value instanceof Value.Str) {
            String errorMsg = value.expectString();
            if (errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
                errorMsg += ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.";
            }
            throw SdkClientException.create(errorMsg);
        } else {
            throw SdkClientException.create("Rule engine return neither an endpoint result or error value. Returned value was: "
                                            + value);
        }
    }

    private static Rule endpointRule_2() {
        return Rule
            .builder()
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("isSet").argv(Arrays.asList(Expr.ref(Identifier.of("useFIPSEndpoint"))))
                              .build().validate()).build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("booleanEquals")
                              .argv(Arrays.asList(Expr.ref(Identifier.of("useFIPSEndpoint")), Expr.of(true))).build()
                              .validate()).build()).error("FIPS endpoints not supported with multi-region endpoints");
    }

    private static Rule endpointRule_3() {
        return Rule
            .builder()
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode
                            .builder()
                            .fn("not")
                            .argv(Arrays.asList(FnNode.builder().fn("isSet")
                                                      .argv(Arrays.asList(Expr.ref(Identifier.of("useFIPSEndpoint")))).build()
                                                      .validate())).build().validate()).build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("isSet")
                              .argv(Arrays.asList(Expr.ref(Identifier.of("useDualStackEndpoint")))).build().validate())
                    .build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("booleanEquals")
                              .argv(Arrays.asList(Expr.ref(Identifier.of("useDualStackEndpoint")), Expr.of(true)))
                              .build().validate()).build())
            .endpoint(
                EndpointResult
                    .builder()
                    .url(Expr.of("https://{endpointId}.query.{partitionResult#dualStackDnsSuffix}"))
                    .addProperty(
                        Identifier.of("authSchemes"),
                        Literal.fromTuple(Arrays.asList(Literal.fromRecord(MapUtils.of(Identifier.of("name"),
                                                                                       Literal.fromStr("sigv4a"), Identifier.of("signingName"),
                                                                                       Literal.fromStr("query"), Identifier.of("signingRegionSet"),
                                                                                       Literal.fromTuple(Arrays.asList(Literal.fromStr("*")))))))).build());
    }

    private static Rule endpointRule_4() {
        return Rule.builder()
                   .endpoint(
                       EndpointResult
                           .builder()
                           .url(Expr.of("https://{endpointId}.query.{partitionResult#dnsSuffix}"))
                           .addProperty(
                               Identifier.of("authSchemes"),
                               Literal.fromTuple(Arrays.asList(Literal.fromRecord(MapUtils.of(Identifier.of("name"),
                                                                                              Literal.fromStr("sigv4a"), Identifier.of("signingName"),
                                                                                              Literal.fromStr("query"), Identifier.of("signingRegionSet"),
                                                                                              Literal.fromTuple(Arrays.asList(Literal.fromStr("*")))))))).build());
    }

    private static Rule endpointRule_1() {
        return Rule
            .builder()
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("isSet").argv(Arrays.asList(Expr.ref(Identifier.of("endpointId"))))
                              .build().validate()).build())
            .treeRule(Arrays.asList(endpointRule_2(), endpointRule_3(), endpointRule_4()));
    }

    private static Rule endpointRule_6() {
        return Rule
            .builder()
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("isSet").argv(Arrays.asList(Expr.ref(Identifier.of("useFIPSEndpoint"))))
                              .build().validate()).build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("booleanEquals")
                              .argv(Arrays.asList(Expr.ref(Identifier.of("useFIPSEndpoint")), Expr.of(true))).build()
                              .validate()).build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode
                            .builder()
                            .fn("not")
                            .argv(Arrays.asList(FnNode.builder().fn("isSet")
                                                      .argv(Arrays.asList(Expr.ref(Identifier.of("useDualStackEndpoint")))).build()
                                                      .validate())).build().validate()).build())
            .endpoint(
                EndpointResult
                    .builder()
                    .url(Expr.of("https://query-fips.{region}.{partitionResult#dnsSuffix}"))
                    .addProperty(
                        Identifier.of("authSchemes"),
                        Literal.fromTuple(Arrays.asList(Literal.fromRecord(MapUtils.of(Identifier.of("name"),
                                                                                       Literal.fromStr("sigv4a"), Identifier.of("signingName"),
                                                                                       Literal.fromStr("query"), Identifier.of("signingRegionSet"),
                                                                                       Literal.fromTuple(Arrays.asList(Literal.fromStr("*")))))))).build());
    }

    private static Rule endpointRule_7() {
        return Rule
            .builder()
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("isSet")
                              .argv(Arrays.asList(Expr.ref(Identifier.of("useDualStackEndpoint")))).build().validate())
                    .build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("booleanEquals")
                              .argv(Arrays.asList(Expr.ref(Identifier.of("useDualStackEndpoint")), Expr.of(true)))
                              .build().validate()).build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode
                            .builder()
                            .fn("not")
                            .argv(Arrays.asList(FnNode.builder().fn("isSet")
                                                      .argv(Arrays.asList(Expr.ref(Identifier.of("useFIPSEndpoint")))).build()
                                                      .validate())).build().validate()).build())
            .endpoint(
                EndpointResult
                    .builder()
                    .url(Expr.of("https://query.{region}.{partitionResult#dualStackDnsSuffix}"))
                    .addProperty(
                        Identifier.of("authSchemes"),
                        Literal.fromTuple(Arrays.asList(Literal.fromRecord(MapUtils.of(Identifier.of("name"),
                                                                                       Literal.fromStr("sigv4a"), Identifier.of("signingName"),
                                                                                       Literal.fromStr("query"), Identifier.of("signingRegionSet"),
                                                                                       Literal.fromTuple(Arrays.asList(Literal.fromStr("*"))))), Literal
                                                            .fromRecord(MapUtils.of(Identifier.of("name"), Literal.fromStr("sigv4"),
                                                                                    Identifier.of("signingName"), Literal.fromStr("query"),
                                                                                    Identifier.of("signingRegion"), Literal.fromStr("{region}")))))).build());
    }

    private static Rule endpointRule_8() {
        return Rule
            .builder()
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("isSet")
                              .argv(Arrays.asList(Expr.ref(Identifier.of("useDualStackEndpoint")))).build().validate())
                    .build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("isSet").argv(Arrays.asList(Expr.ref(Identifier.of("useFIPSEndpoint"))))
                              .build().validate()).build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("booleanEquals")
                              .argv(Arrays.asList(Expr.ref(Identifier.of("useDualStackEndpoint")), Expr.of(true)))
                              .build().validate()).build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("booleanEquals")
                              .argv(Arrays.asList(Expr.ref(Identifier.of("useFIPSEndpoint")), Expr.of(true))).build()
                              .validate()).build())
            .endpoint(
                EndpointResult
                    .builder()
                    .url(Expr.of("https://query-fips.{region}.{partitionResult#dualStackDnsSuffix}"))
                    .addProperty(
                        Identifier.of("authSchemes"),
                        Literal.fromTuple(Arrays.asList(Literal.fromRecord(MapUtils.of(Identifier.of("name"),
                                                                                       Literal.fromStr("sigv4a"), Identifier.of("signingName"),
                                                                                       Literal.fromStr("query"), Identifier.of("signingRegionSet"),
                                                                                       Literal.fromTuple(Arrays.asList(Literal.fromStr("*")))))))).build());
    }

    private static Rule endpointRule_9() {
        return Rule.builder().endpoint(
            EndpointResult.builder().url(Expr.of("https://query.{region}.{partitionResult#dnsSuffix}")).build());
    }

    private static Rule endpointRule_5() {
        return Rule
            .builder()
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("isValidHostLabel")
                              .argv(Arrays.asList(Expr.ref(Identifier.of("region")), Expr.of(false))).build()
                              .validate()).build())
            .treeRule(Arrays.asList(endpointRule_6(), endpointRule_7(), endpointRule_8(), endpointRule_9()));
    }

    private static Rule endpointRule_10() {
        return Rule.builder().error("{region} is not a valid HTTP host-label");
    }

    private static Rule endpointRule_11() {
        return Rule
            .builder()
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode
                            .builder()
                            .fn("not")
                            .argv(Arrays.asList(FnNode.builder().fn("isSet")
                                                      .argv(Arrays.asList(Expr.ref(Identifier.of("useFIPSEndpoint")))).build()
                                                      .validate())).build().validate()).build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("isSet")
                              .argv(Arrays.asList(Expr.ref(Identifier.of("useDualStackEndpoint")))).build().validate())
                    .build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("booleanEquals")
                              .argv(Arrays.asList(Expr.ref(Identifier.of("useDualStackEndpoint")), Expr.of(true)))
                              .build().validate()).build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("isSet").argv(Arrays.asList(Expr.ref(Identifier.of("ArnList")))).build()
                              .validate()).build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("getAttr")
                              .argv(Arrays.asList(Expr.ref(Identifier.of("ArnList")), Expr.of("[0]"))).build()
                              .validate()).result("FirstArn").build())
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("aws.parseArn").argv(Arrays.asList(Expr.ref(Identifier.of("FirstArn"))))
                              .build().validate()).result("ParsedArn").build())
            .endpoint(
                EndpointResult
                    .builder()
                    .url(Expr.of("https://{endpointId}.query.{partitionResult#dualStackDnsSuffix}"))
                    .addProperty(
                        Identifier.of("authSchemes"),
                        Literal.fromTuple(Arrays.asList(Literal.fromRecord(MapUtils.of(Identifier.of("name"),
                                                                                       Literal.fromStr("sigv4a"), Identifier.of("signingName"),
                                                                                       Literal.fromStr("query"), Identifier.of("signingRegionSet"),
                                                                                       Literal.fromTuple(Arrays.asList(Literal.fromStr("*")))))))).build());
    }

    private static Rule endpointRule_0() {
        return Rule
            .builder()
            .addCondition(
                Condition
                    .builder()
                    .fn(FnNode.builder().fn("aws.partition").argv(Arrays.asList(Expr.ref(Identifier.of("region"))))
                              .build().validate()).result("partitionResult").build())
            .treeRule(Arrays.asList(endpointRule_1(), endpointRule_5(), endpointRule_10(), endpointRule_11()));
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
                                 .required(false).builtIn("AWS::UseDualStack").build())
                    .addParameter(
                        Parameter.builder().name("useFIPSEndpoint").type(ParameterType.fromValue("boolean"))
                                 .required(false).builtIn("AWS::UseFIPS").build())
                    .addParameter(
                        Parameter.builder().name("AccountId").type(ParameterType.fromValue("String"))
                                 .required(false).builtIn("AWS::Auth::AccountId").build())
                    .addParameter(
                        Parameter.builder().name("AccountIdEndpointMode").type(ParameterType.fromValue("String"))
                                 .required(false).builtIn("AWS::Auth::AccountIdEndpointMode").build())
                    .addParameter(
                        Parameter.builder().name("listOfStrings").type(ParameterType.fromValue("StringArray"))
                                 .required(false).build())
                    .addParameter(
                        Parameter
                            .builder()
                            .name("defaultListOfStrings")
                            .type(ParameterType.fromValue("stringarray"))
                            .required(false)
                            .defaultValue(
                                Value.fromArray(Arrays.asList("item1", "item2", "item3").stream()
                                                      .map(Value::fromStr).collect(Collectors.toList()))).build())
                    .addParameter(
                        Parameter.builder().name("endpointId").type(ParameterType.fromValue("string"))
                                 .required(false).build())
                    .addParameter(
                        Parameter.builder().name("defaultTrueParam").type(ParameterType.fromValue("boolean"))
                                 .required(false).documentation("A param that defauls to true")
                                 .defaultValue(Value.fromBool(true)).build())
                    .addParameter(
                        Parameter.builder().name("defaultStringParam").type(ParameterType.fromValue("string"))
                                 .required(false).defaultValue(Value.fromStr("hello endpoints")).build())
                    .addParameter(
                        Parameter.builder().name("deprecatedParam").type(ParameterType.fromValue("string"))
                                 .required(false).deprecated(new Parameter.Deprecated("Don't use!", "2021-01-01"))
                                 .build())
                    .addParameter(
                        Parameter.builder().name("booleanContextParam").type(ParameterType.fromValue("boolean"))
                                 .required(false).build())
                    .addParameter(
                        Parameter.builder().name("stringContextParam").type(ParameterType.fromValue("string"))
                                 .required(false).build())
                    .addParameter(
                        Parameter.builder().name("operationContextParam").type(ParameterType.fromValue("string"))
                                 .required(false).build())
                    .addParameter(
                        Parameter.builder().name("CustomEndpointArray")
                                 .type(ParameterType.fromValue("StringArray")).required(false)
                                 .documentation("Parameter from the customization config").build())
                    .addParameter(
                        Parameter.builder().name("ArnList").type(ParameterType.fromValue("StringArray"))
                                 .required(false).documentation("Parameter from the customization config").build())
                    .build()).addRule(endpointRule_0()).build();
    }

    @Override
    public boolean equals(Object rhs) {
        return rhs != null && getClass().equals(rhs.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    private void addKnownProperties(Endpoint.Builder builder, Map<String, Value> properties) {
        properties.forEach((n, v) -> {
            switch (n) {
                case "authSchemes":
                    builder.putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, endpointAuthSchemeStrategy.createAuthSchemes(v));
                    break;
                default:
                    LOG.debug(() -> "Ignoring unknown endpoint property: " + n);
                    break;
            }
        });
    }
}
