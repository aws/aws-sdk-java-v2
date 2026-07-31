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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Generates the BDD (binary decision diagram) based endpoint provider.
 *
 * <p>The BDD is emitted as direct Java control flow rather than a table-driven traversal loop: each BDD node becomes an
 * {@code if (cond<k>()) ... else ...} branch. This keeps condition evaluation and branch selection in straight-line,
 * JIT-friendly code with one branch-predictor site per node, matching the shape (and performance) of the classic
 * rules-based resolver instead of paying an uninlineable switch dispatch per node visit.
 *
 * <p>Nodes referenced by more than one parent (the BDD is a DAG) are emitted once as a {@code node<i>()} method and
 * invoked from each referencing site. Single-parent nodes are inlined into their parent, subject to
 * {@link #MAX_INLINE_NODES_PER_METHOD} so no generated method approaches the JVM's huge-method compilation limit.
 */
public class BddEndpointProviderSpec implements ClassSpec {
    /**
     * Node references at or above this value denote results; {@code ref - RESULT_OFFSET} is the result index.
     */
    private static final int RESULT_OFFSET = 100000001;

    /**
     * Maximum number of BDD nodes inlined into a single generated method. Each inlined node contributes roughly
     * 20-30 bytes of bytecode, so 100 nodes keeps methods a comfortable order of magnitude below HotSpot's
     * 8000-byte "huge method" limit (above which methods are never JIT-compiled by default) while still producing
     * long straight-line runs for the JIT.
     */
    private static final int MAX_INLINE_NODES_PER_METHOD = 100;

    private final IntermediateModel intermediateModel;
    private final EndpointBddModel endpointBddModel;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;
    private final Map<String, KeyTypePair> knownEndpointAttributes;
    private final RuleRuntimeTypeMirror typeMirror;
    private final Map<String, RegistryInfo> registerInfoMap;
    private final ClassName evaluatorType;
    private final List<EndpointBddModel.BddNode> bddNodes;

    public BddEndpointProviderSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.endpointBddModel = intermediateModel.getEndpointBddModel();
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
        String packageName = intermediateModel.getMetadata().getFullInternalEndpointRulesPackageName();
        this.typeMirror = new RuleRuntimeTypeMirror(packageName);
        this.knownEndpointAttributes = knownEndpointAttributes(intermediateModel);
        this.registerInfoMap = buildRegisterInfoMap();
        this.evaluatorType = className().nestedClass("Evaluator");
        this.bddNodes = endpointBddModel.getDecodedNodes();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className())
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addSuperinterface(endpointRulesSpecUtils.providerInterfaceName())
                                            .addType(evaluatorClass())
                                            .addAnnotation(SdkInternalApi.class);

        builder.addMethod(resolveEndpointMethod());
        return builder.build();
    }

    private TypeSpec evaluatorClass() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(evaluatorType)
                                          .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                          .addField(FieldSpec
                                                        .builder(endpointRulesSpecUtils.parametersClassName(), "params")
                                                        .build());

        registerInfoMap.forEach((k, r) -> {
            TypeName type = r.getRuleType().type();
            if (type.isPrimitive() && r.isNullable()) {
                type = type.box();
            }
            if (!r.isNonRegionParam()) {
                builder.addField(
                    FieldSpec
                        .builder(type, r.getName())
                        .build()
                );
            }
        });

        builder.addMethods(bddMethods());
        builder.addMethods(conditionFns());
        builder.addMethods(resultFns());
        return builder.build();
    }

    /**
     * Generates the {@code evaluate()} entry point plus one {@code node<i>()} method for every BDD node that is either
     * shared (multiple parents) or was split out of its parent to respect {@link #MAX_INLINE_NODES_PER_METHOD}.
     */
    private List<MethodSpec> bddMethods() {
        Set<Integer> sharedUnits = computeSharedUnits();
        // Units promoted to methods, in discovery order for deterministic output.
        Set<Integer> methodUnits = new LinkedHashSet<>();
        Deque<Integer> workList = new ArrayDeque<>();

        List<MethodSpec> methods = new ArrayList<>();

        CodeBlock.Builder rootBody = CodeBlock.builder();
        emitTarget(endpointBddModel.getRoot(), rootBody, new int[] {0}, sharedUnits, methodUnits, workList);
        methods.add(MethodSpec.methodBuilder("evaluate")
                              .addModifiers(Modifier.PUBLIC)
                              .returns(typeMirror.rulesResult().type())
                              .addCode(rootBody.build())
                              .build());

        Set<Integer> emitted = new LinkedHashSet<>();
        while (!workList.isEmpty()) {
            int unit = workList.poll();
            if (!emitted.add(unit)) {
                continue;
            }
            CodeBlock.Builder body = CodeBlock.builder();
            // The unit itself counts against the method's inline budget.
            emitNode(unit, body, new int[] {1}, sharedUnits, methodUnits, workList);
            methods.add(MethodSpec.methodBuilder(unitMethodName(unit))
                                  .addModifiers(Modifier.PRIVATE)
                                  .returns(typeMirror.rulesResult().type())
                                  .addCode(body.build())
                                  .build());
        }
        return methods;
    }

    /**
     * Emits the code for reaching {@code ref}: either a terminal, a result, a call to a {@code node<i>()} method, or
     * the inlined branch code of the referenced node. All emitted paths end in a {@code return}.
     */
    private void emitTarget(int ref, CodeBlock.Builder builder, int[] inlineCount,
                            Set<Integer> sharedUnits, Set<Integer> methodUnits, Deque<Integer> workList) {
        if (ref == 1 || ref == -1) {
            // Terminal: no endpoint or error result was reached.
            builder.addStatement("return $T.carryOn()", typeMirror.rulesResult().type());
            return;
        }
        if (isResultRef(ref)) {
            builder.addStatement("return result$L()", ref - RESULT_OFFSET);
            return;
        }
        int unit = unitKey(ref);
        if (sharedUnits.contains(unit) || methodUnits.contains(unit) || inlineCount[0] >= MAX_INLINE_NODES_PER_METHOD) {
            if (methodUnits.add(unit)) {
                workList.add(unit);
            }
            builder.addStatement("return $L()", unitMethodName(unit));
            return;
        }
        inlineCount[0]++;
        emitNode(unit, builder, inlineCount, sharedUnits, methodUnits, workList);
    }

    /**
     * Emits the branch code of a single BDD node: evaluate its condition, then continue to the high edge on true and
     * the low edge on false. A complemented reference swaps the edges.
     */
    private void emitNode(int unit, CodeBlock.Builder builder, int[] inlineCount,
                          Set<Integer> sharedUnits, Set<Integer> methodUnits, Deque<Integer> workList) {
        EndpointBddModel.BddNode node = bddNodes.get(unitNodeIndex(unit));
        boolean complemented = isComplementedUnit(unit);
        int highRef = complemented ? node.getLowRef() : node.getHighRef();
        int lowRef = complemented ? node.getHighRef() : node.getLowRef();

        builder.beginControlFlow("if (cond$L())", node.getConditionIndex());
        emitTarget(highRef, builder, inlineCount, sharedUnits, methodUnits, workList);
        builder.endControlFlow();
        emitTarget(lowRef, builder, inlineCount, sharedUnits, methodUnits, workList);
    }

    /**
     * Returns the set of node units referenced by more than one parent edge in the graph reachable from the root.
     * These are always emitted as methods so their code is generated only once.
     */
    private Set<Integer> computeSharedUnits() {
        Map<Integer, Integer> refCounts = new HashMap<>();
        Set<Integer> visited = new LinkedHashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        int root = endpointBddModel.getRoot();
        if (isNodeRef(root)) {
            int rootUnit = unitKey(root);
            refCounts.put(rootUnit, 1);
            stack.push(rootUnit);
        }
        while (!stack.isEmpty()) {
            int unit = stack.pop();
            if (!visited.add(unit)) {
                continue;
            }
            EndpointBddModel.BddNode node = bddNodes.get(unitNodeIndex(unit));
            for (int ref : new int[] {node.getHighRef(), node.getLowRef()}) {
                if (isNodeRef(ref)) {
                    int childUnit = unitKey(ref);
                    refCounts.merge(childUnit, 1, Integer::sum);
                    stack.push(childUnit);
                }
            }
        }
        Set<Integer> shared = new LinkedHashSet<>();
        refCounts.forEach((unit, count) -> {
            if (count > 1) {
                shared.add(unit);
            }
        });
        return shared;
    }

    private static boolean isNodeRef(int ref) {
        return (ref > 1 || ref < -1) && !isResultRef(ref);
    }

    private static boolean isResultRef(int ref) {
        return ref >= RESULT_OFFSET;
    }

    /**
     * A "unit" identifies a node together with the polarity it is referenced with: a complemented reference evaluates
     * the same node with its edges swapped, and so generates distinct code.
     */
    private static int unitKey(int ref) {
        int nodeIndex = (ref < 0 ? -ref : ref) - 1;
        return nodeIndex * 2 + (ref < 0 ? 1 : 0);
    }

    private static int unitNodeIndex(int unit) {
        return unit / 2;
    }

    private static boolean isComplementedUnit(int unit) {
        return (unit & 1) == 1;
    }

    private static String unitMethodName(int unit) {
        return "node" + unitNodeIndex(unit) + (isComplementedUnit(unit) ? "C" : "");
    }

    private List<MethodSpec> conditionFns() {
        List<MethodSpec> methods = new ArrayList<>();
        for (int cI = 0; cI < endpointBddModel.getConditions().size(); cI++) {
            CodeBlock.Builder codeBuilder = CodeBlock.builder();
            ConditionModel c = endpointBddModel.getConditions().get(cI);
            // hack for now to work around ExpressionParser
            RuleModel synthetic = new RuleModel();
            synthetic.setType("error");
            synthetic.setError("synthetic");
            synthetic.setConditions(Collections.singletonList(c));
            RuleExpression parsedSynthetic = ExpressionParser
                .parseRuleSetExpression(synthetic)
                .accept(new PrepareForCodegenVisitor());
            parsedSynthetic.accept(new ConditionFnCodeGeneratorVisitor(codeBuilder, typeMirror, registerInfoMap,
                                                                       endpointRulesSpecUtils));
            methods.add(MethodSpec
                            .methodBuilder("cond" + cI)
                            .addModifiers(Modifier.PRIVATE)
                            .returns(boolean.class)
                            .addCode(codeBuilder.build())
                            .build());
        }
        return methods;
    }

    private List<MethodSpec> resultFns() {
        List<MethodSpec> methods = new ArrayList<>();
        for (int rI = 0; rI < endpointBddModel.getResults().size(); rI++) {
            CodeBlock.Builder codeBuilder = CodeBlock.builder();
            RuleExpression parsedSynthetic = ExpressionParser
                .parseRuleSetExpression(endpointBddModel.getResults().get(rI))
                .accept(new PrepareForCodegenVisitor());
            parsedSynthetic.accept(new ResultFnCodeGeneratorVisitor(
                codeBuilder, typeMirror, registerInfoMap, knownEndpointAttributes, endpointRulesSpecUtils,
                intermediateModel.getCustomizationConfig().useS3ExpressSessionAuth()));
            methods.add(MethodSpec
                            .methodBuilder("result" + rI)
                            .addModifiers(Modifier.PRIVATE)
                            .returns(typeMirror.rulesResult().type())
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
                                              .addParameter(endpointRulesSpecUtils.parametersClassName(), "params");

        builder.addCode(validateRequiredParams());
        builder.addStatement("$T evaluator = new $T()", evaluatorType, evaluatorType);

        // region builtin parameter needs to be mapped from Region to String
        String regionParamName = regionParamName();
        if (regionParamName != null) {
            String regionMethodName = endpointRulesSpecUtils.paramMethodName(regionParamName);
            builder.addStatement("evaluator.$L = params.$L() == null ? null : params.$L().id()",
                                 registerInfoMap.get(regionParamName).getName(),
                                 regionMethodName, regionMethodName);
        }

        builder.addStatement("evaluator.params = params");
        builder.addStatement("$T result = evaluator.evaluate()", typeMirror.rulesResult().type());

        builder.beginControlFlow("if (result.isEndpoint())")
               .addStatement("return $T.completedFuture(result.endpoint())", CompletableFuture.class)
               .endControlFlow();
        builder.beginControlFlow("if (result.isError())")
               .addStatement("String errorMsg = result.error()")
               .beginControlFlow("if (errorMsg.contains(\"Invalid ARN\") && errorMsg.contains(\":s3:::\"))")
               .addStatement("errorMsg += $S", ". Use the bucket name instead of simple bucket ARNs in "
                                               + "GetBucketLocationRequest.")
               .endControlFlow()
               .addStatement("return $T.failedFuture($T.create(errorMsg))", CompletableFutureUtils.class,
                             SdkClientException.class)
               .endControlFlow();
        builder.addStatement("return $T.failedFuture($T.create($S))", CompletableFutureUtils.class,
                             SdkClientException.class, "Rule engine did not reach an error or endpoint result");

        return builder.build();
    }

    @Override
    public ClassName className() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullInternalEndpointRulesPackageName(),
                             "Default" + endpointRulesSpecUtils.providerInterfaceName().simpleName());
    }

    // return the name of the region param (mapped to region builtin). Returns null if none set.
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
                                     "params",
                                     endpointRulesSpecUtils.paramMethodName(e.getKey()),
                                     String.format("Parameter '%s' must not be null", e.getKey()));
                  });
        return b.build();
    }

    private Map<String, RegistryInfo> buildRegisterInfoMap() {
        int index = 0;
        Map<String, RegistryInfo> registryInfo = new LinkedHashMap<>();
        // first add an entry for every parameter
        for (Map.Entry<String, ParameterModel> entry : endpointBddModel.getParameters().entrySet()) {
            String name = intermediateModel.getNamingStrategy().getVariableName(entry.getKey());
            boolean nullable = entry.getValue().getDefault() == null;
            boolean isRegionBuiltIn = entry.getValue().getBuiltInEnum() == BuiltInParameter.AWS_REGION;
            String nonRegionParamKey = isRegionBuiltIn ? null : entry.getKey();
            registryInfo.put(
                entry.getKey(),
                new RegistryInfo(name, index, fromParameterModel(entry.getValue()),
                                 null, nullable, nonRegionParamKey));
            index += 1;
        }
        // add an entry for every assigned variable. assigns are guaranteed to be globally unique
        for (ConditionModel conditionModel : endpointBddModel.getConditions()) {
            if (conditionModel.getAssign() != null) {
                // at this point we don't know the type.
                // Create a RulesetExpression that will be used to infer the type using the visitor
                RuleModel synthetic = new RuleModel();
                synthetic.setType("error");
                synthetic.setError("synthetic");
                synthetic.setConditions(Collections.singletonList(conditionModel));
                String name = intermediateModel.getNamingStrategy().getVariableName(conditionModel.getAssign());
                registryInfo.put(
                    conditionModel.getAssign(),
                    new RegistryInfo(name, index, ExpressionParser.parseRuleSetExpression(synthetic)));
                index += 1;
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
}
