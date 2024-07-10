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

package software.amazon.awssdk.codegen.poet.rules;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collections;
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
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.RuleModel;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

public class EndpointProviderSpec implements ClassSpec {
    private final IntermediateModel intermediateModel;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;
    private final Map<String, KeyTypePair> knownEndpointAttributes;
    private final CodegenExpressionBuidler utils;
    private final RuleRuntimeTypeMirror typeMirror;

    public EndpointProviderSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(intermediateModel);
        String packageName = intermediateModel.getMetadata().getFullInternalEndpointRulesPackageName();
        this.typeMirror = new RuleRuntimeTypeMirror(packageName);
        EndpointRuleSetModel model = intermediateModel.getEndpointRuleSetModel();
        this.utils = createCodegenRulesUtil(model.getRules(), model.getParameters(), typeMirror);
        this.knownEndpointAttributes = knownEndpointAttributes(intermediateModel);
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

    private static RuleModel createRootRule(List<RuleModel> rules) {
        RuleModel root = new RuleModel();
        root.setRules(rules);
        root.setType("tree");
        root.setConditions(Collections.emptyList());
        return root;
    }

    private static CodegenExpressionBuidler createCodegenRulesUtil(List<RuleModel> rules,
                                                                   Map<String, ParameterModel> parameters,
                                                                   RuleRuntimeTypeMirror typeMirror) {
        RuleSetExpression root = ExpressionParser.parseRuleSetExpression(createRootRule(rules));
        return CodegenExpressionBuidler.from(root, typeMirror, initSymbolTable(parameters));
    }

    private static SymbolTable initSymbolTable(Map<String, ParameterModel> parameters) {
        SymbolTable.Builder builder = SymbolTable.builder();
        parameters.forEach((k, v) -> {
            builder.putParam(k, fromParameterModel(v), v.getBuiltInEnum());
        });
        return builder.build();
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

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className())
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addSuperinterface(endpointRulesSpecUtils.providerInterfaceName())
                                            .addAnnotation(SdkInternalApi.class);

        builder.addType(codegenLocalState());
        builder.addType(codegenLocalStateBuilder());
        builder.addMethod(resolveEndpointMethod());
        List<MethodSpec.Builder> methods = new ArrayList<>();
        createRuleMethod(utils.root(), methods);
        for (MethodSpec.Builder methodBuilder : methods) {
            builder.addMethod(methodBuilder.build());
        }
        builder.addMethod(equalsMethod());
        builder.addMethod(hashCodeMethod());
        return builder.build();
    }

    @Override
    public ClassName className() {
        Metadata md = intermediateModel.getMetadata();
        return ClassName.get(md.getFullInternalEndpointRulesPackageName(),
                             "Default" + endpointRulesSpecUtils.providerInterfaceName().simpleName());
    }

    private MethodSpec resolveEndpointMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveEndpoint")
                                               .addModifiers(Modifier.PUBLIC)
                                               .returns(endpointRulesSpecUtils.resolverReturnType())
                                               .addAnnotation(Override.class)
                                               .addParameter(endpointRulesSpecUtils.parametersClassName(), "params");

        builder.addCode(validateRequiredParams());
        builder.beginControlFlow("try");
        builder.addStatement("$T result = $L(params, new $T())", ruleResult(), utils.root().ruleId(),
                             ClassName.bestGuess("LocalState"));
        builder.beginControlFlow("if (result.canContinue())")
               .addStatement("throw $T.create($S)", SdkClientException.class, "Rule engine did not reach an error or "
                                                                              + "endpoint result")
               .endControlFlow();

        builder.beginControlFlow("if (result.isError())")
               .addStatement("String errorMsg = result.error()")
               .beginControlFlow("if (errorMsg.contains(\"Invalid ARN\") && errorMsg.contains(\":s3:::\"))")
               .addStatement("errorMsg += $S", ". Use the bucket name instead of simple bucket ARNs in "
                                               + "GetBucketLocationRequest.")
               .endControlFlow()
               .addStatement("throw $T.create(errorMsg)", SdkClientException.class)
               .endControlFlow();

        builder.addStatement("return $T.completedFuture(result.endpoint())", CompletableFuture.class);
        builder.nextControlFlow("catch ($T error)", Exception.class);
        builder.addStatement("return $T.failedFuture(error)", CompletableFutureUtils.class);
        builder.endControlFlow();

        return builder.build();
    }

    private CodeBlock validateRequiredParams() {
        CodeBlock.Builder b = CodeBlock.builder();
        Map<String, ParameterModel> parameters = intermediateModel.getEndpointRuleSetModel().getParameters();
        parameters.entrySet().stream()
                  .filter(e -> Boolean.TRUE.equals(e.getValue().isRequired()))
                  .forEach(e -> {
                      b.addStatement("$T.notNull($N.$N(), $S)",
                                     Validate.class,
                                     "params",
                                     endpointRulesSpecUtils.paramMethodName(e.getKey()),
                                     String.format("Parameter '%s' must not be null", e.getKey()));
                  });

        return b.build();
    }

    private void createRuleMethod(RuleSetExpression expr, List<MethodSpec.Builder> methods) {
        MethodSpec.Builder builder = methodBuilderForRule(expr);
        methods.add(builder);
        CodeBlock.Builder block = CodeBlock.builder();
        codegenExpr(expr, block);
        builder.addCode(block.build());
        if (expr.isTree()) {
            for (RuleSetExpression child : expr.children()) {
                createRuleMethod(child, methods);
            }
        }
    }

    private MethodSpec.Builder methodBuilderForRule(RuleSetExpression expr) {
        MethodSpec.Builder builder =
            MethodSpec.methodBuilder(expr.ruleId())
                      .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                      .returns(ruleResult())
                      .addParameter(endpointRulesSpecUtils.parametersClassName(), "params");
        builder.addParameter(ClassName.bestGuess("LocalState"), "locals");
        return builder;
    }

    private void codegenExpr(RuleExpression expr, CodeBlock.Builder builder) {
        CodeGeneratorVisitor visitor = new CodeGeneratorVisitor(typeMirror,
                                                                utils.symbolTable(),
                                                                knownEndpointAttributes,
                                                                builder);
        expr.accept(visitor);
    }

    private TypeSpec codegenLocalState() {
        TypeSpec.Builder b = TypeSpec.classBuilder("LocalState")
                                     .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
        Map<String, RuleType> locals = utils.locals();
        locals.forEach((k, v) -> {
            b.addField(v.javaType(), k, Modifier.PRIVATE, Modifier.FINAL);
        });
        MethodSpec.Builder emptyCtor = MethodSpec.constructorBuilder();
        locals.forEach((k, v) -> {
            emptyCtor.addStatement("this.$1L = null", k);
        });
        b.addMethod(emptyCtor.build());
        ClassName localStateBuilder = ClassName.bestGuess("LocalStateBuilder");
        MethodSpec.Builder builderCtor = MethodSpec
            .constructorBuilder()
            .addParameter(localStateBuilder, "builder");

        locals.forEach((k, v) -> {
            builderCtor.addStatement("this.$1L = builder.$1L", k);
        });

        b.addMethod(builderCtor.build());
        locals.forEach((k, v) -> {
            b.addMethod(MethodSpec.methodBuilder(k)
                                  .addModifiers(Modifier.PUBLIC)
                                  .returns(v.javaType())
                                  .addStatement("return this.$L", k)
                                  .build());
        });
        b.addMethod(MethodSpec.methodBuilder("toBuilder")
                              .addModifiers(Modifier.PUBLIC)
                              .returns(localStateBuilder)
                              .addStatement("return new $T(this)", localStateBuilder)
                              .build());
        return b.build();
    }

    private TypeSpec codegenLocalStateBuilder() {
        ClassName localStateClass = ClassName.bestGuess("LocalState");
        ClassName builderClass = ClassName.bestGuess("LocalStateBuilder");
        TypeSpec.Builder b = TypeSpec.classBuilder("LocalStateBuilder")
                                     .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
        Map<String, RuleType> locals = utils.locals();
        locals.forEach((k, v) -> {
            b.addField(v.javaType(), k, Modifier.PRIVATE);
        });
        MethodSpec.Builder emptyCtor = MethodSpec.constructorBuilder();
        locals.forEach((k, v) -> {
            emptyCtor.addStatement("this.$1L = null", k);
        });
        b.addMethod(emptyCtor.build());
        MethodSpec.Builder stateCtor = MethodSpec
            .constructorBuilder()
            .addParameter(localStateClass, "locals");
        locals.forEach((k, v) -> {
            stateCtor.addStatement("this.$1L = locals.$1L", k);
        });
        b.addMethod(stateCtor.build());
        locals.forEach((k, v) -> {
            b.addMethod(MethodSpec.methodBuilder(k)
                                  .addModifiers(Modifier.PUBLIC)
                                  .returns(builderClass)
                                  .addParameter(v.javaType(), "value")
                                  .addStatement("this.$L = value", k)
                                  .addStatement("return this")
                                  .build());
        });
        b.addMethod(MethodSpec.methodBuilder("build")
                              .returns(localStateClass)
                              .addStatement("return new $T(this)", localStateClass)
                              .build());
        return b.build();
    }

    private TypeName ruleResult() {
        return typeMirror.rulesResult().type();
    }

    private MethodSpec equalsMethod() {
        return MethodSpec.methodBuilder("equals")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(boolean.class)
                         .addParameter(Object.class, "rhs")
                         .addStatement("return rhs != null && getClass().equals(rhs.getClass())")
                         .build();
    }

    private MethodSpec hashCodeMethod() {
        return MethodSpec.methodBuilder("hashCode")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(int.class)
                         .addStatement("return getClass().hashCode()")
                         .build();
    }
}
