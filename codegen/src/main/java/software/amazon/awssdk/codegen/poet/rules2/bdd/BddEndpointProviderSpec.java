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

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.jr.stree.JrsValue;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.EndpointAuthSchemeConfig;
import software.amazon.awssdk.codegen.model.config.customization.KeyTypePair;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.rules.endpoints.BuiltInParameter;
import software.amazon.awssdk.codegen.model.rules.endpoints.ConditionModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.RuleModel;
import software.amazon.awssdk.codegen.model.service.EndpointBddModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.codegen.poet.rules2.ExpressionParser;
import software.amazon.awssdk.codegen.poet.rules2.PrepareForCodegenVisitor;
import software.amazon.awssdk.codegen.poet.rules2.RuleExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleRuntimeTypeMirror;
import software.amazon.awssdk.codegen.poet.rules2.RuleType;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Generates the BDD (binary decision diagram) based endpoint provider using a smithy-java-inspired
 * approach: each BDD node is emitted as a {@code nodeP<i>} method that returns {@code Endpoint}
 * directly (null for no-match). When a complement edge references a node, a {@code nodeN<i>}
 * variant is also emitted with swapped branches (condition true → lowRef, false → highRef).
 * Simple conditions (isSet, booleanEquals, stringEquals on plain register references) are inlined
 * as ternary expressions. Complex conditions that compute and store values are emitted as separate
 * {@code cond<i>()} methods.
 *
 * <p>The evaluator is allocated per call (lightweight — just register fields, no maps). The shared
 * RulesFunctions helpers it calls are stateless.
 */
public class BddEndpointProviderSpec implements ClassSpec {
    /**
     * Node references at or above this value denote results; {@code ref - RESULT_OFFSET} is the result index.
     * The SEP defines 100_000_000 as the implicit NoMatchRule. The model's results list excludes NoMatchRule,
     * so result indices start at 100_000_001 (result 0 in the list).
     */
    private static final int RESULT_OFFSET = 100_000_001;

    /**
     * The SEP's implicit NoMatchRule reference — indicates no endpoint matched.
     */
    private static final int NO_MATCH_RESULT = 100_000_000;

    private final IntermediateModel intermediateModel;
    private final EndpointBddModel endpointBddModel;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;
    private final Map<String, KeyTypePair> knownEndpointAttributes;
    private final RuleRuntimeTypeMirror typeMirror;
    private final Map<String, RegistryInfo> registerInfoMap;
    private final ClassName evaluatorType;
    private final ClassName cacheEntryType;
    private final List<EndpointBddModel.BddNode> bddNodes;
    private final List<ConditionType> conditionTypes;
    private final EndpointProviderCacheIndex cacheIndex;

    public BddEndpointProviderSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.endpointBddModel = intermediateModel.getEndpointBddModel();
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
        String packageName = intermediateModel.getMetadata().getFullInternalEndpointRulesPackageName();
        this.typeMirror = new RuleRuntimeTypeMirror(packageName);
        this.knownEndpointAttributes = knownEndpointAttributes(intermediateModel);
        this.registerInfoMap = buildRegisterInfoMap();
        this.evaluatorType = className().nestedClass("Evaluator");
        this.cacheEntryType = className().nestedClass("CacheEntry");
        this.bddNodes = endpointBddModel.getDecodedNodes();
        this.conditionTypes = analyzeConditions();
        this.cacheIndex = EndpointProviderCacheIndex.of(intermediateModel);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className())
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addSuperinterface(endpointRulesSpecUtils.providerInterfaceName())
                                            .addAnnotation(SdkInternalApi.class);

        builder.addField(cacheField());
        builder.addType(evaluatorClass());
        builder.addType(cacheEntryClass());
        builder.addMethod(resolveEndpointMethod());
        builder.addMethod(cacheParamsMatchMethod());

        return builder.build();
    }

    // ---- Single-entry result cache ----

    /**
     * Generates {@code private volatile CacheEntry cache;}.
     *
     * <p>One entry, holding the most recent successfully resolved {@code (params, endpoint)} pair. A single entry is
     * enough because the overwhelmingly common shape is a client resolving the same endpoint repeatedly: same region,
     * same flags, and for most services no request-derived parameters at all. A service whose endpoint genuinely varies
     * per request simply misses every time and pays only the key check.
     *
     * <p>{@code volatile} is the whole of the synchronisation. Racing threads compute equivalent entries for equal
     * params, so a lost write costs one re-resolution and nothing more; {@code CacheEntry} is immutable with final
     * fields, so a thread that reads the reference sees fully initialised contents.
     */
    private FieldSpec cacheField() {
        return FieldSpec.builder(cacheEntryType, "cache")
                        .addModifiers(Modifier.PRIVATE, Modifier.VOLATILE)
                        .build();
    }

    /**
     * Generates the immutable {@code CacheEntry} holding one {@code (params, endpoint)} snapshot.
     */
    private TypeSpec cacheEntryClass() {
        ClassName paramsClass = endpointRulesSpecUtils.parametersClassName();
        return TypeSpec.classBuilder(cacheEntryType)
                       .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                       .addField(paramsClass, "params", Modifier.FINAL)
                       .addField(Endpoint.class, "endpoint", Modifier.FINAL)
                       .addMethod(MethodSpec.constructorBuilder()
                                            .addParameter(paramsClass, "params")
                                            .addParameter(Endpoint.class, "endpoint")
                                            .addStatement("this.params = params")
                                            .addStatement("this.endpoint = endpoint")
                                            .build())
                       .build();
    }

    /**
     * Generates {@code cacheParamsMatch(a, b)}: true when the two parameter objects are interchangeable as far as
     * endpoint resolution is concerned.
     *
     * <p>Every parameter the BDD model declares is compared. Parameters are ordered by
     * {@link EndpointCacheKeyClassification}, cheapest comparison first, and the method returns on the first mismatch.
     *
     * <p>A parameter that {@link EndpointProviderCacheIndex} classified but that the model no longer declares would be
     * a hole in the key, so that combination fails codegen instead of generating a comparison that quietly skips it.
     */
    private MethodSpec cacheParamsMatchMethod() {
        ClassName paramsClass = endpointRulesSpecUtils.parametersClassName();
        Map<String, ParameterModel> parameters = endpointBddModel.getParameters();

        MethodSpec.Builder b = MethodSpec.methodBuilder("cacheParamsMatch")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .returns(boolean.class)
                                         .addParameter(paramsClass, "a")
                                         .addParameter(paramsClass, "b");

        int listIndex = 0;
        for (Map.Entry<String, EndpointCacheKeyClassification> entry : cacheIndex.classifiedParameters().entrySet()) {
            String paramName = entry.getKey();
            if (!parameters.containsKey(paramName)) {
                throw new IllegalStateException(
                    "Endpoint parameter '" + paramName + "' was classified for the result cache but is not declared by "
                    + "the BDD model. Leaving it out of the cache key would let the provider return an endpoint "
                    + "resolved for a different value of it.");
            }
            String getter = endpointRulesSpecUtils.paramMethodName(paramName) + "()";
            if (entry.getValue() == EndpointCacheKeyClassification.REQUEST_LIST) {
                addListParamCheck(b, getter, listIndex++);
            } else {
                addScalarParamCheck(b, getter, entry.getValue());
            }
        }

        b.addStatement("return true");
        return b.build();
    }

    /**
     * Emits the comparison for one non-list parameter, returning false on mismatch.
     *
     * <p>{@code BOOLEAN}, {@code CLIENT_STATIC_REF} and {@code OPERATION_STATIC} compare references only. For booleans
     * that is complete, not just fast: autoboxing hands back the {@code Boolean.TRUE}/{@code Boolean.FALSE} singletons.
     * For the other two the SDK hands the same reference to every request, and if it ever does not, the result is a
     * miss and a re-resolution rather than a wrong endpoint.
     *
     * <p>The remaining classifications add an {@code equals} fallback so that an equal value arriving as a fresh
     * reference still hits.
     */
    private static void addScalarParamCheck(MethodSpec.Builder b, String getter, EndpointCacheKeyClassification cat) {
        switch (cat) {
            case BOOLEAN:
            case CLIENT_STATIC_REF:
            case OPERATION_STATIC:
                b.addStatement("if (a.$L != b.$L) return false", getter, getter);
                break;
            default:
                b.beginControlFlow("if (a.$L != b.$L)", getter, getter);
                b.beginControlFlow("if (a.$L == null || !a.$L.equals(b.$L))", getter, getter, getter);
                b.addStatement("return false");
                b.endControlFlow();
                b.endControlFlow();
                break;
        }
    }

    /**
     * Emits the comparison for one {@code stringArray} parameter: identity, then null, then size, then the element
     * walk. The size cap keeps the check bounded so a request carrying a large list cannot make the cache check itself
     * a cost worth avoiding.
     *
     * <p>{@code idx} suffixes the local variable names so several list parameters can be compared in one method.
     */
    private static void addListParamCheck(MethodSpec.Builder b, String getter, int idx) {
        String listA = "listA" + idx;
        String listB = "listB" + idx;
        String i = "i" + idx;
        String elementA = "elementA" + idx;
        String elementB = "elementB" + idx;
        TypeName listOfString = RuleRuntimeTypeMirror.LIST_OF_STRING.type();

        b.addStatement("$T $L = a.$L", listOfString, listA, getter);
        b.addStatement("$T $L = b.$L", listOfString, listB, getter);
        b.beginControlFlow("if ($L != $L)", listA, listB);
        b.addStatement("if ($L == null || $L == null) return false", listA, listB);
        b.addStatement("if ($L.size() != $L.size()) return false", listA, listB);
        b.addStatement("if ($L.size() > $L) return false", listA, EndpointProviderCacheIndex.MAX_LIST_COMPARISON_SIZE);
        b.beginControlFlow("for (int $L = 0; $L < $L.size(); $L++)", i, i, listA, i);
        b.addStatement("$T $L = $L.get($L)", String.class, elementA, listA, i);
        b.addStatement("$T $L = $L.get($L)", String.class, elementB, listB, i);
        b.addStatement("if ($L != $L && ($L == null || !$L.equals($L))) return false",
                       elementA, elementB, elementA, elementA, elementB);
        b.endControlFlow();
        b.endControlFlow();
    }

    private TypeSpec evaluatorClass() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(evaluatorType)
                                          .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

        // params field
        builder.addField(FieldSpec.builder(endpointRulesSpecUtils.parametersClassName(), "params").build());

        // Register fields
        registerInfoMap.forEach((k, r) -> {
            TypeName type = r.getRuleType().type();
            if (type.isPrimitive() && r.isNullable()) {
                type = type.box();
            }
            if (!r.isNonRegionParam()) {
                builder.addField(FieldSpec.builder(type, r.getName()).build());
            }
        });

        // BDD node methods
        builder.addMethods(bddNodeMethods());

        // Condition methods (only for complex conditions)
        builder.addMethods(conditionMethods());

        // Result methods
        builder.addMethods(resultMethods());

        return builder.build();
    }

    /**
     * Generates methods for each BDD node. For each node, emits:
     * <ul>
     *   <li>{@code nodeP<i>} — positive edge: condition true → highRef, false → lowRef</li>
     *   <li>{@code nodeN<i>} — complement edge: condition true → lowRef, false → highRef (branches swapped)</li>
     * </ul>
     *
     * <p>Complement edges allow the BDD to share nodes: a negative reference {@code -N} means
     * "evaluate node N-1 with inverted condition result." Only nodeN variants that are actually
     * referenced are emitted to avoid dead code.
     *
     * <p>Simple conditions are inlined as ternary expressions. Complex conditions call cond<i>().
     */
    private List<MethodSpec> bddNodeMethods() {
        // First pass: determine which nodes need a complement (nodeN) variant
        boolean[] needsNodeN = new boolean[bddNodes.size()];
        for (EndpointBddModel.BddNode node : bddNodes) {
            markComplementRef(needsNodeN, node.getHighRef());
            markComplementRef(needsNodeN, node.getLowRef());
        }
        // Also check the root reference
        markComplementRef(needsNodeN, endpointBddModel.getRoot());

        List<MethodSpec> methods = new ArrayList<>();

        for (int i = 0; i < bddNodes.size(); i++) {
            EndpointBddModel.BddNode node = bddNodes.get(i);
            int condIdx = node.getConditionIndex();

            // Node 0 is the terminal sentinel (conditionIndex = -1) — returns null (no match)
            if (i == 0) {
                if (condIdx != -1) {
                    throw new IllegalStateException(
                        "BDD node 0 must be the terminal sentinel with conditionIndex=-1, got: " + condIdx);
                }
                methods.add(MethodSpec.methodBuilder("nodeP0")
                                      .addModifiers(Modifier.PRIVATE)
                                      .returns(Endpoint.class)
                                      .addStatement("return null")
                                      .build());
                if (needsNodeN[0]) {
                    methods.add(MethodSpec.methodBuilder("nodeN0")
                                          .addModifiers(Modifier.PRIVATE)
                                          .returns(Endpoint.class)
                                          .addStatement("return null")
                                          .build());
                }
                continue;
            }

            // All other nodes must have a valid condition index
            if (condIdx < 0 || condIdx >= conditionTypes.size()) {
                throw new IllegalStateException(
                    "BDD node " + i + " has invalid conditionIndex=" + condIdx
                    + " (valid range: 0.." + (conditionTypes.size() - 1) + ")");
            }

            ConditionType ct = conditionTypes.get(condIdx);

            // nodeP: condition true → highRef, false → lowRef
            methods.add(MethodSpec.methodBuilder("nodeP" + i)
                                  .addModifiers(Modifier.PRIVATE)
                                  .returns(Endpoint.class)
                                  .addCode(nodeBody(ct, condIdx, node.getHighRef(), node.getLowRef()))
                                  .build());

            // nodeN: complement — condition true → lowRef, false → highRef (swapped)
            if (needsNodeN[i]) {
                methods.add(MethodSpec.methodBuilder("nodeN" + i)
                                      .addModifiers(Modifier.PRIVATE)
                                      .returns(Endpoint.class)
                                      .addCode(nodeBody(ct, condIdx, node.getLowRef(), node.getHighRef()))
                                      .build());
            }
        }

        return methods;
    }

    /**
     * Marks a node as needing a complement (nodeN) variant if the reference is a negative node ref.
     */
    private void markComplementRef(boolean[] needsNodeN, int ref) {
        if (ref < -1) {
            int nodeIndex = (-ref) - 1;
            if (nodeIndex < needsNodeN.length) {
                needsNodeN[nodeIndex] = true;
            }
        }
    }

    /**
     * Generates the body of a nodeP method as a ternary return statement.
     */
    private CodeBlock nodeBody(ConditionType ct, int condIdx, int highRef, int lowRef) {
        CodeBlock.Builder code = CodeBlock.builder();
        String condExpr = conditionExpression(ct, condIdx);
        code.addStatement("return $L\n    ? $L\n    : $L",
                          condExpr,
                          referenceExpression(highRef),
                          referenceExpression(lowRef));
        return code.build();
    }

    /**
     * Returns the Java expression for evaluating a condition.
     * Simple conditions are inlined; complex ones call cond<i>().
     */
    private String conditionExpression(ConditionType ct, int condIdx) {
        switch (ct.kind) {
            case ISSET:
                return ct.registerName + " != null";
            case BOOL_TRUE:
                return "Boolean.TRUE.equals(" + ct.registerName + ")";
            case BOOL_FALSE:
                return "Boolean.FALSE.equals(" + ct.registerName + ")";
            case STRING_EQ:
                return ct.registerName + " != null && " + ct.registerName + ".equals(" + ct.stringConstant + ")";
            case COMPLEX:
            default:
                return "cond" + condIdx + "()";
        }
    }

    /**
     * Returns the Java expression for a BDD reference: a node call, a result call, or null (terminal/no-match).
     * A negative reference (other than -1) is a complement edge: {@code -N} means "call nodeN<N-1>()"
     * which evaluates the same condition but swaps the branch targets.
     */
    private String referenceExpression(int ref) {
        // Terminal refs: SEP defines 1 and -1 as terminals (true/false sinks)
        if (ref == 1 || ref == -1) {
            return "null";
        }
        // NoMatchRule: SEP defines 100_000_000 as implicit no-match
        if (ref == NO_MATCH_RESULT) {
            return "null";
        }
        // Result refs
        if (ref >= RESULT_OFFSET) {
            int resultIndex = ref - RESULT_OFFSET;
            if (resultIndex >= endpointBddModel.getResults().size()) {
                throw new IllegalStateException(
                    "BDD result reference " + ref + " maps to index " + resultIndex
                    + " but only " + endpointBddModel.getResults().size() + " results exist");
            }
            return "result" + resultIndex + "()";
        }
        // Invalid ref
        if (ref == 0) {
            throw new IllegalStateException("BDD reference 0 is invalid (node indices are 1-based)");
        }
        // Complement edge: negative ref → nodeN (swapped branches)
        if (ref < 0) {
            int nodeIndex = (-ref) - 1;
            if (nodeIndex >= bddNodes.size()) {
                throw new IllegalStateException(
                    "BDD complement reference " + ref + " maps to node index " + nodeIndex
                    + " but only " + bddNodes.size() + " nodes exist");
            }
            return "nodeN" + nodeIndex + "()";
        }
        // Positive node ref (1-based → 0-based)
        int nodeIndex = ref - 1;
        if (nodeIndex >= bddNodes.size()) {
            throw new IllegalStateException(
                "BDD node reference " + ref + " maps to index " + nodeIndex
                + " but only " + bddNodes.size() + " nodes exist");
        }
        return "nodeP" + nodeIndex + "()";
    }

    /**
     * Generates condition methods only for complex conditions (those that compute and store values).
     */
    private List<MethodSpec> conditionMethods() {
        List<MethodSpec> methods = new ArrayList<>();
        for (int cI = 0; cI < endpointBddModel.getConditions().size(); cI++) {
            if (conditionTypes.get(cI).kind != ConditionKind.COMPLEX) {
                continue;
            }
            CodeBlock.Builder codeBuilder = CodeBlock.builder();
            ConditionModel c = endpointBddModel.getConditions().get(cI);
            // Use existing expression parser for complex conditions
            RuleModel synthetic = new RuleModel();
            synthetic.setType("error");
            synthetic.setError("synthetic");
            synthetic.setConditions(Collections.singletonList(c));
            RuleExpression parsedSynthetic = ExpressionParser
                .parseRuleSetExpression(synthetic)
                .accept(new BddPeepholeVisitor())
                .accept(new PrepareForCodegenVisitor());
            parsedSynthetic.accept(new ConditionFnCodeGeneratorVisitor(codeBuilder, typeMirror, registerInfoMap,
                                                                       endpointRulesSpecUtils));
            methods.add(MethodSpec.methodBuilder("cond" + cI)
                                  .addModifiers(Modifier.PRIVATE)
                                  .returns(boolean.class)
                                  .addCode(codeBuilder.build())
                                  .build());
        }
        return methods;
    }

    /**
     * Generates result methods that return Endpoint directly or throw SdkClientException for errors.
     */
    private List<MethodSpec> resultMethods() {
        List<MethodSpec> methods = new ArrayList<>();
        for (int rI = 0; rI < endpointBddModel.getResults().size(); rI++) {
            CodeBlock.Builder codeBuilder = CodeBlock.builder();
            // BddPeepholeVisitor is deliberately not applied here. It rewrites condition-shaped
            // expressions (stringEquals, coalesce-with-boolean-default, ite, isValidHostLabel), and a
            // BDD result cannot contain any: the BDD hoists all computation into conditions, so
            // results only consume already-assigned registers. BddResultCodeGeneratorVisitor enforces
            // that by rejecting conditions and let-bindings outright. Applying the peephole here
            // produced identical output while requiring a duplicate copy of every emitter.
            RuleExpression parsedSynthetic = ExpressionParser
                .parseRuleSetExpression(endpointBddModel.getResults().get(rI))
                .accept(new PrepareForCodegenVisitor());
            parsedSynthetic.accept(new BddResultCodeGeneratorVisitor(
                codeBuilder, typeMirror, registerInfoMap, knownEndpointAttributes, endpointRulesSpecUtils,
                intermediateModel.getCustomizationConfig().useS3ExpressSessionAuth()));
            methods.add(MethodSpec.methodBuilder("result" + rI)
                                  .addModifiers(Modifier.PRIVATE)
                                  .returns(Endpoint.class)
                                  .addCode(codeBuilder.build())
                                  .build());
        }
        return methods;
    }

    private MethodSpec resolveEndpointMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveEndpoint")
                                              .addModifiers(Modifier.PUBLIC)
                                              .returns(endpointRulesSpecUtils.resolverReturnType())
                                              .addAnnotation(Override.class)
                                              .addParameter(endpointRulesSpecUtils.parametersClassName(), "endpointParams");

        builder.addCode(validateRequiredParams());

        // Cache check. This sits after required-param validation so that invalid params fail the same way on a hit as
        // on a miss. One volatile read into a local, so the entry cannot be replaced between the null check and the
        // comparison.
        builder.addComment("Single-entry result cache: reuse the last endpoint when the params still match.");
        builder.addStatement("$T cached = this.cache", cacheEntryType);
        builder.beginControlFlow("if (cached != null && cacheParamsMatch(endpointParams, cached.params))");
        builder.addStatement("return $T.completedFuture(cached.endpoint)", CompletableFuture.class);
        builder.endControlFlow();

        builder.beginControlFlow("try");

        // Allocate evaluator per call — lightweight (just fields, no maps), immediately young-gen collected.
        builder.addStatement("$T evaluator = new $T()", evaluatorType, evaluatorType);

        // Initialize evaluator from params
        builder.addStatement("evaluator.params = endpointParams");
        String regionParamName = regionParamName();
        if (regionParamName != null) {
            String regionMethodName = endpointRulesSpecUtils.paramMethodName(regionParamName);
            builder.addStatement("evaluator.$L = endpointParams.$L() == null ? null : endpointParams.$L().id()",
                                 registerInfoMap.get(regionParamName).getName(),
                                 regionMethodName, regionMethodName);
        }

        // Evaluate BDD — returns Endpoint directly (null = no match)
        builder.addStatement("$T result = evaluator.$L",
                             Endpoint.class, referenceExpression(endpointBddModel.getRoot()));
        builder.beginControlFlow("if (result == null)")
               .addStatement("return $T.failedFuture($T.create($S))",
                             CompletableFutureUtils.class, SdkClientException.class,
                             "Rule engine did not reach an error or endpoint result")
               .endControlFlow();
        // Populate on success only. A rule error and a no-match both leave the previous entry in place, so a transient
        // bad-params call cannot poison the cache and an error is never replayed from it.
        builder.addStatement("this.cache = new $T(endpointParams, result)", cacheEntryType);
        builder.addStatement("return $T.completedFuture(result)", CompletableFuture.class);

        // Catch errors thrown from result methods
        builder.nextControlFlow("catch ($T e)", SdkClientException.class);
        builder.addStatement("String errorMsg = e.getMessage()");
        builder.beginControlFlow("if (errorMsg != null && errorMsg.contains(\"Invalid ARN\") && errorMsg.contains(\":s3:::\"))")
               .addStatement("return $T.failedFuture($T.create(errorMsg + $S, e))",
                             CompletableFutureUtils.class, SdkClientException.class,
                             ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.")
               .endControlFlow();
        builder.addStatement("return $T.failedFuture(e)", CompletableFutureUtils.class);

        builder.nextControlFlow("catch ($T error)", Exception.class);
        builder.addStatement("return $T.failedFuture(error)", CompletableFutureUtils.class);

        builder.endControlFlow();

        return builder.build();
    }

    @Override
    public ClassName className() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullInternalEndpointRulesPackageName(),
                             "Default" + endpointRulesSpecUtils.providerInterfaceName().simpleName());
    }

    // ---- Condition analysis ----

    /**
     * Analyzes each condition in the BDD model to determine if it can be inlined in a node method
     * or needs a full cond<i>() method.
     */
    private List<ConditionType> analyzeConditions() {
        List<ConditionType> types = new ArrayList<>();
        for (ConditionModel condition : endpointBddModel.getConditions()) {
            types.add(classifyCondition(condition));
        }
        return types;
    }

    private ConditionType classifyCondition(ConditionModel condition) {
        // Conditions with assign always need a method (they have side effects)
        if (condition.getAssign() != null) {
            return ConditionType.complex();
        }

        String fn = condition.getFn();
        List<TreeNode> argv = condition.getArgv();

        // isSet({ref}) -> ISSET
        if ("isSet".equals(fn) && argv.size() == 1 && isSimpleRef(argv.get(0))) {
            String refName = getRefName(argv.get(0));
            return ConditionType.isSet(resolveRegisterAccessExpression(refName));
        }

        // booleanEquals({ref}, true/false) -> BOOL_TRUE or BOOL_FALSE
        if ("booleanEquals".equals(fn) && argv.size() == 2 && isSimpleRef(argv.get(0)) && isBooleanLiteral(argv.get(1))) {
            String refName = getRefName(argv.get(0));
            boolean value = getBooleanValue(argv.get(1));
            String registerExpr = resolveRegisterAccessExpression(refName);
            return value ? ConditionType.boolTrue(registerExpr) : ConditionType.boolFalse(registerExpr);
        }

        // stringEquals({ref}, "literal") -> STRING_EQ
        if ("stringEquals".equals(fn) && argv.size() == 2 && isSimpleRef(argv.get(0)) && isStringLiteral(argv.get(1))) {
            String refName = getRefName(argv.get(0));
            String literal = getStringValue(argv.get(1));
            String registerExpr = resolveRegisterAccessExpression(refName);
            // Quote the string literal for use in generated code
            String quotedLiteral = "\"" + escapeJavaString(literal) + "\"";
            return ConditionType.stringEq(registerExpr, quotedLiteral);
        }

        return ConditionType.complex();
    }

    /**
     * Resolves a parameter/register name to the Java expression that accesses it in the Evaluator.
     * For non-region params, this is {@code params.xxx()}; for registers, it's the field name.
     */
    private String resolveRegisterAccessExpression(String name) {
        RegistryInfo info = registerInfoMap.get(name);
        if (info == null) {
            // Fallback — shouldn't happen for well-formed models
            return intermediateModel.getNamingStrategy().getVariableName(name);
        }
        if (info.isNonRegionParam()) {
            return "params." + endpointRulesSpecUtils.paramMethodName(info.getNonRegionParamKey()) + "()";
        }
        return info.getName();
    }

    private static boolean isSimpleRef(TreeNode node) {
        // A simple ref is: {"ref": "SomeName"} — an object with exactly one field "ref" that is a string value
        if (!node.isObject() || node.size() != 1) {
            return false;
        }
        TreeNode refNode = node.get("ref");
        return refNode != null && refNode.isValueNode() && refNode.asToken() == JsonToken.VALUE_STRING;
    }

    private static String getRefName(TreeNode node) {
        return ((JrsValue) node.get("ref")).asText();
    }

    private static boolean isBooleanLiteral(TreeNode node) {
        if (!node.isValueNode()) {
            return false;
        }
        JsonToken token = node.asToken();
        return token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE;
    }

    private static boolean getBooleanValue(TreeNode node) {
        return node.asToken() == JsonToken.VALUE_TRUE;
    }

    private static boolean isStringLiteral(TreeNode node) {
        return node.isValueNode() && node.asToken() == JsonToken.VALUE_STRING;
    }

    private static String getStringValue(TreeNode node) {
        return ((JrsValue) node).asText();
    }

    private static String escapeJavaString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    // ---- Helpers ----

    private String regionParamName() {
        for (Map.Entry<String, ParameterModel> entry : endpointBddModel.getParameters().entrySet()) {
            if (entry.getValue().getBuiltInEnum() == BuiltInParameter.AWS_REGION) {
                return entry.getKey();
            }
        }
        return null;
    }

    private CodeBlock validateRequiredParams() {
        CodeBlock.Builder b = CodeBlock.builder();
        Map<String, ParameterModel> parameters = endpointBddModel.getParameters();
        parameters.entrySet().stream()
                  .filter(e -> Boolean.TRUE.equals(e.getValue().isRequired()) && e.getValue().getDefault() == null)
                  .forEach(e -> {
                      b.addStatement("$T.notNull($N.$N(), $S)",
                                     Validate.class,
                                     "endpointParams",
                                     endpointRulesSpecUtils.paramMethodName(e.getKey()),
                                     String.format("Parameter '%s' must not be null", e.getKey()));
                  });
        return b.build();
    }

    private Map<String, RegistryInfo> buildRegisterInfoMap() {
        Map<String, RegistryInfo> registryInfo = new LinkedHashMap<>();
        // first add an entry for every parameter
        for (Map.Entry<String, ParameterModel> entry : endpointBddModel.getParameters().entrySet()) {
            String name = intermediateModel.getNamingStrategy().getVariableName(entry.getKey());
            boolean nullable = entry.getValue().getDefault() == null;
            boolean isRegionBuiltIn = entry.getValue().getBuiltInEnum() == BuiltInParameter.AWS_REGION;
            String nonRegionParamKey = isRegionBuiltIn ? null : entry.getKey();
            registryInfo.put(
                entry.getKey(),
                new RegistryInfo(name, fromParameterModel(entry.getValue()),
                                 null, nullable, nonRegionParamKey));
        }
        // add an entry for every assigned variable. assigns are guaranteed to be globally unique
        for (ConditionModel conditionModel : endpointBddModel.getConditions()) {
            if (conditionModel.getAssign() != null) {
                RuleModel synthetic = new RuleModel();
                synthetic.setType("error");
                synthetic.setError("synthetic");
                synthetic.setConditions(Collections.singletonList(conditionModel));
                String name = intermediateModel.getNamingStrategy().getVariableName(conditionModel.getAssign());
                registryInfo.put(
                    conditionModel.getAssign(),
                    new RegistryInfo(name, ExpressionParser.parseRuleSetExpression(synthetic)));
            }
        }
        // visit all the conditions/assignments and infer types
        AssignTypeInferringVisitor typeVisitor = new AssignTypeInferringVisitor(typeMirror, registryInfo);
        registryInfo.values().forEach(r -> {
            if (r.getRuleSetExpression() != null) {
                r.getRuleSetExpression().accept(typeVisitor);
            }
        });
        // assert that we have type information for all registry values
        registryInfo.values().forEach(r -> {
            if (r.getRuleType() == null) {
                throw new IllegalStateException("Unable to infer type for `" + r.getName() + "`");
            }
        });
        return Collections.unmodifiableMap(registryInfo);
    }

    private static RuleType fromParameterModel(ParameterModel model) {
        switch (model.getType().toLowerCase(Locale.ENGLISH)) {
            case "boolean":
                return RuleRuntimeTypeMirror.BOOLEAN;
            case "string":
                return RuleRuntimeTypeMirror.STRING;
            case "stringarray":
                return RuleRuntimeTypeMirror.LIST_OF_STRING;
            default:
                throw new IllegalStateException("Cannot find rule type for: " + model.getType());
        }
    }

    private static Map<String, KeyTypePair> knownEndpointAttributes(IntermediateModel intermediateModel) {
        Map<String, KeyTypePair> knownEndpointAttributes = null;
        EndpointAuthSchemeConfig config = intermediateModel.getCustomizationConfig().getEndpointAuthSchemeConfig();
        if (config != null) {
            knownEndpointAttributes = config.getEndpointProviderTestKeys();
        }
        if (knownEndpointAttributes == null) {
            knownEndpointAttributes = Collections.emptyMap();
        }
        return knownEndpointAttributes;
    }

    // ---- Condition type classification ----

    enum ConditionKind {
        ISSET,
        BOOL_TRUE,
        BOOL_FALSE,
        STRING_EQ,
        COMPLEX
    }

    static class ConditionType {
        final ConditionKind kind;
        final String registerName;  // Java expression to access the register/param
        final String stringConstant; // Only for STRING_EQ

        private ConditionType(ConditionKind kind, String registerName, String stringConstant) {
            this.kind = kind;
            this.registerName = registerName;
            this.stringConstant = stringConstant;
        }

        static ConditionType complex() {
            return new ConditionType(ConditionKind.COMPLEX, null, null);
        }

        static ConditionType isSet(String registerName) {
            return new ConditionType(ConditionKind.ISSET, registerName, null);
        }

        static ConditionType boolTrue(String registerName) {
            return new ConditionType(ConditionKind.BOOL_TRUE, registerName, null);
        }

        static ConditionType boolFalse(String registerName) {
            return new ConditionType(ConditionKind.BOOL_FALSE, registerName, null);
        }

        static ConditionType stringEq(String registerName, String stringConstant) {
            return new ConditionType(ConditionKind.STRING_EQ, registerName, stringConstant);
        }
    }
}
