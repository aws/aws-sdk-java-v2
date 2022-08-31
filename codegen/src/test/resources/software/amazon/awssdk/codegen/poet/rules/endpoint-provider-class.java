package software.amazon.awssdk.services.query.rules.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.rules.DefaultRuleEngine;
import software.amazon.awssdk.core.rules.EndpointRuleset;
import software.amazon.awssdk.core.rules.Identifier;
import software.amazon.awssdk.core.rules.RuleEngine;
import software.amazon.awssdk.core.rules.Value;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.services.query.rules.QueryEndpointParameters;
import software.amazon.awssdk.services.query.rules.QueryEndpointProvider;
import software.amazon.awssdk.utils.Lazy;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryEndpointProvider implements QueryEndpointProvider {
    private static final Lazy<EndpointRuleset> ENDPOINT_RULE_SET = new Lazy<>(DefaultQueryEndpointProvider::loadRuleSet);

    private static final RuleEngine RULES_ENGINE = new DefaultRuleEngine();

    @Override
    public Endpoint resolveEndpoint(QueryEndpointParameters endpointParams) {
        RULES_ENGINE.evaluate(ENDPOINT_RULE_SET.getValue(), toIdentifierValueMap(endpointParams));
    }

    private static EndpointRuleset loadRuleSet() {
        try (InputStream is = DefaultQueryEndpointProvider.class
            .getResourceAsStream("/software/amazon/awssdk/services/query/internal/endpoint-rule-set.json")) {
            return EndpointRuleset.fromNode(JsonNode.parser().parse(is));
        } catch (IOException e) {
            throw SdkClientException.create("Unable to close input stream", e);
        }
    }

    private static Map<Identifier, Value> toIdentifierValueMap(QueryEndpointParameters params) {
        Map<Identifier, Value> paramsMap = new HashMap<>();
        String region = params.region();
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
        return paramsMap;
    }
}
