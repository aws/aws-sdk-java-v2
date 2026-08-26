/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.codegen.poet.rules2.bdd;

import static software.amazon.awssdk.codegen.poet.rules2.bdd.EndpointCacheKeyClassification.BOOLEAN;
import static software.amazon.awssdk.codegen.poet.rules2.bdd.EndpointCacheKeyClassification.CLIENT_STATIC_REF;
import static software.amazon.awssdk.codegen.poet.rules2.bdd.EndpointCacheKeyClassification.IDENTITY_DERIVED;
import static software.amazon.awssdk.codegen.poet.rules2.bdd.EndpointCacheKeyClassification.OPERATION_STATIC;
import static software.amazon.awssdk.codegen.poet.rules2.bdd.EndpointCacheKeyClassification.REQUEST_DYNAMIC;
import static software.amazon.awssdk.codegen.poet.rules2.bdd.EndpointCacheKeyClassification.REQUEST_LIST;
import static software.amazon.awssdk.codegen.poet.rules2.bdd.EndpointCacheKeyClassification.SEMI_STABLE;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.BuiltInParameter;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.service.ClientContextParam;
import software.amazon.awssdk.codegen.model.service.ContextParam;
import software.amazon.awssdk.codegen.model.service.StaticContextParam;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Computes the {@link EndpointCacheKeyClassification} of every endpoint parameter, at codegen time, for
 * {@code BddEndpointProviderSpec} to turn into the tiered {@code cacheParamsMatch} method inside
 * {@code Default{Service}EndpointProvider}.
 *
 * <h2>Parameter source</h2>
 *
 * <p>Parameters come from the <em>BDD</em> model, not the rule set, because the BDD is what the generated provider
 * evaluates. The two agree for a service whose BDD was compiled from its own rule set, but nothing in codegen enforces
 * that, and the codegen test models deliberately pair mismatched files. Reading the rule set here would silently drop
 * every parameter the BDD declares and the rule set does not, and a parameter missing from the cache key is the one way
 * this cache can hand back an endpoint resolved for different inputs.
 *
 * <h2>Classification</h2>
 *
 * <ol>
 *   <li>Seed each parameter from its own declaration: type first, then built-in, then
 *       {@code clientContextParams} membership.</li>
 *   <li>Scan every operation for binding sites ({@code contextParam}, {@code staticContextParams},
 *       {@code operationContextParams}) and promote the parameter to the more dynamic classification. Most dynamic
 *       wins, so a parameter bound statically by one operation and from the request by another is compared the way the
 *       request-bound operation needs.</li>
 * </ol>
 *
 * <p>The returned map is ordered by classification, cheapest first, then by name within a classification. That ordering
 * is the generated comparison order, so it is deterministic across builds.
 */
public final class EndpointProviderCacheIndex {
    /**
     * Lists longer than this report a cache miss without element-wise comparison, bounding the cost of the key check
     * regardless of request size. Eight covers the list-valued endpoint parameters shipped today while keeping the
     * worst-case check comfortably cheaper than a re-resolution.
     */
    public static final int MAX_LIST_COMPARISON_SIZE = 8;

    private final IntermediateModel model;

    private EndpointProviderCacheIndex(IntermediateModel model) {
        this.model = model;
    }

    public static EndpointProviderCacheIndex of(IntermediateModel model) {
        return new EndpointProviderCacheIndex(model);
    }

    /**
     * Returns the more dynamic of two classifications. Used with {@link Map#merge} to implement most-dynamic-wins.
     */
    public static EndpointCacheKeyClassification moreExpensive(EndpointCacheKeyClassification existing,
                                                               EndpointCacheKeyClassification candidate) {
        return candidate.ordinal() > existing.ordinal() ? candidate : existing;
    }

    /**
     * Returns an unmodifiable map from parameter name to classification, ordered cheapest comparison first.
     */
    public Map<String, EndpointCacheKeyClassification> classifiedParameters() {
        Map<String, ParameterModel> parameters = model.getEndpointBddModel().getParameters();
        Map<String, ClientContextParam> clientContextParams = model.getClientContextParams();

        // A null value means "not decided from the declaration alone; let the binding sites decide". Map.merge treats a
        // null value as absent, so the first binding site simply wins and later ones promote from there.
        Map<String, EndpointCacheKeyClassification> result = new LinkedHashMap<>();
        parameters.forEach((name, pm) -> result.put(name, initialClassification(name, pm, clientContextParams)));

        for (OperationModel op : model.getOperations().values()) {
            promoteForContextParams(op, parameters, result);
            promoteForStaticContextParams(op, parameters, result);
            promoteForOperationContextParams(op, parameters, result);
        }

        // No binding site named it, so we cannot say where its value comes from. Assume per-request.
        result.replaceAll((name, category) -> category == null ? REQUEST_DYNAMIC : category);

        Map<String, EndpointCacheKeyClassification> sorted = new LinkedHashMap<>();
        result.entrySet().stream()
              .sorted((a, b) -> {
                  int cmp = Integer.compare(a.getValue().ordinal(), b.getValue().ordinal());
                  return cmp != 0 ? cmp : a.getKey().compareTo(b.getKey());
              })
              .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return Collections.unmodifiableMap(sorted);
    }

    /**
     * A {@code contextParam} binds the parameter to a member of the request, so its value arrives fresh per request.
     */
    private static void promoteForContextParams(OperationModel op,
                                                Map<String, ParameterModel> parameters,
                                                Map<String, EndpointCacheKeyClassification> result) {
        if (op.getInputShape() == null) {
            return;
        }
        for (MemberModel member : op.getInputShape().getMembers()) {
            ContextParam cp = member.getContextParam();
            if (cp == null) {
                continue;
            }
            String paramName = findParamName(parameters, cp.getName());
            if (paramName != null) {
                result.merge(paramName, REQUEST_DYNAMIC, EndpointProviderCacheIndex::moreExpensive);
            }
        }
    }

    /**
     * A {@code staticContextParam} is a literal fixed at codegen time. List values still take the list comparison.
     */
    private static void promoteForStaticContextParams(OperationModel op,
                                                      Map<String, ParameterModel> parameters,
                                                      Map<String, EndpointCacheKeyClassification> result) {
        Map<String, StaticContextParam> statics = op.getStaticContextParams();
        if (CollectionUtils.isNullOrEmpty(statics)) {
            return;
        }
        statics.forEach((paramName, scp) -> {
            String canonicalName = findParamName(parameters, paramName);
            if (canonicalName == null) {
                return;
            }
            EndpointCacheKeyClassification category =
                isList(parameters.get(canonicalName)) ? REQUEST_LIST : OPERATION_STATIC;
            result.merge(canonicalName, category, EndpointProviderCacheIndex::moreExpensive);
        });
    }

    /**
     * An {@code operationContextParam} is a JMESPath expression evaluated over the request, producing a fresh value,
     * and a fresh list when the parameter is a {@code stringArray}.
     */
    private static void promoteForOperationContextParams(OperationModel op,
                                                         Map<String, ParameterModel> parameters,
                                                         Map<String, EndpointCacheKeyClassification> result) {
        if (CollectionUtils.isNullOrEmpty(op.getOperationContextParams())) {
            return;
        }
        op.getOperationContextParams().forEach((paramName, ocp) -> {
            String canonicalName = findParamName(parameters, paramName);
            if (canonicalName == null) {
                return;
            }
            EndpointCacheKeyClassification category =
                isList(parameters.get(canonicalName)) ? REQUEST_LIST : REQUEST_DYNAMIC;
            result.merge(canonicalName, category, EndpointProviderCacheIndex::moreExpensive);
        });
    }

    /**
     * Classifies a parameter from its declaration alone. Returns {@code null} when the declaration does not determine
     * the classification and the operation binding sites should, which is the case for a plain string parameter that is
     * neither a built-in nor a client context param: whether it is an {@code OPERATION_STATIC} literal or arrives from
     * the request is visible only at the binding site.
     */
    private static EndpointCacheKeyClassification initialClassification(String paramName,
                                                                        ParameterModel pm,
                                                                        Map<String, ClientContextParam> clientContextParams) {
        if (isBoolean(pm)) {
            return BOOLEAN;
        }

        if (isList(pm)) {
            return REQUEST_LIST;
        }

        // A built-in wins over a clientContextParams entry that happens to share the parameter's name.
        BuiltInParameter builtIn = pm.getBuiltInEnum();
        if (builtIn != null) {
            return classifyBuiltIn(builtIn);
        }

        if (clientContextParams != null && isClientContextParam(paramName, clientContextParams)) {
            return CLIENT_STATIC_REF;
        }

        return null;
    }

    private static EndpointCacheKeyClassification classifyBuiltIn(BuiltInParameter builtIn) {
        switch (builtIn) {
            case AWS_REGION:
                return CLIENT_STATIC_REF;
            case SDK_ENDPOINT:
            case AWS_AUTH_ACCOUNT_ID_ENDPOINT_MODE:
                return SEMI_STABLE;
            case AWS_AUTH_ACCOUNT_ID:
                return IDENTITY_DERIVED;
            case AWS_USE_DUAL_STACK:
            case AWS_USE_FIPS:
            case AWS_S3_ACCELERATE:
            case AWS_S3_CONTROL_USE_ARN_REGION:
            case AWS_S3_DISABLE_MULTI_REGION_ACCESS_POINTS:
            case AWS_S3_FORCE_PATH_STYLE:
            case AWS_S3_USE_ARN_REGION:
            case AWS_S3_USE_GLOBAL_ENDPOINT:
            case AWS_STS_USE_GLOBAL_ENDPOINT:
                // Every one of these is declared boolean in practice and is intercepted by the boolean check above.
                // Reaching here means a model declared one as a string; they are all client-level config either way,
                // so identity is the right comparison.
                return CLIENT_STATIC_REF;
            default:
                // A built-in added to BuiltInParameter but not yet considered here. Compare with equals as a fallback
                // rather than assuming a stable reference we have not verified.
                return SEMI_STABLE;
        }
    }

    private static boolean isClientContextParam(String paramName, Map<String, ClientContextParam> clientContextParams) {
        if (clientContextParams.containsKey(paramName)) {
            return true;
        }
        for (String key : clientContextParams.keySet()) {
            if (key.equalsIgnoreCase(paramName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBoolean(ParameterModel pm) {
        return "boolean".equalsIgnoreCase(pm.getType());
    }

    private static boolean isList(ParameterModel pm) {
        return "stringarray".equalsIgnoreCase(pm.getType());
    }

    /**
     * Resolves a parameter name as spelled at a binding site to the key used in the parameters map, which may differ in
     * capitalisation. Returns {@code null} when the binding site names a parameter the model does not declare, which is
     * legitimate: a service can bind a context param that its endpoint rules never read.
     */
    private static String findParamName(Map<String, ParameterModel> parameters, String name) {
        if (parameters.containsKey(name)) {
            return name;
        }
        // Endpoint parameter names are unique case-insensitively.
        for (String key : parameters.keySet()) {
            if (key.equalsIgnoreCase(name)) {
                return key;
            }
        }
        return null;
    }
}
