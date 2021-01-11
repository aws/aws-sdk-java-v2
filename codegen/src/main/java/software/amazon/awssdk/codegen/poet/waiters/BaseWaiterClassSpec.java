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

package software.amazon.awssdk.codegen.poet.waiters;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static software.amazon.awssdk.utils.internal.CodegenNamingUtils.lowercaseFirstChar;

import com.fasterxml.jackson.jr.stree.JrsString;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.codegen.emitters.tasks.WaitersRuntimeGeneratorTask;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.Acceptor;
import software.amazon.awssdk.codegen.model.service.WaiterDefinition;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.internal.waiters.WaiterAttribute;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Base class containing common logic shared between the sync waiter class and the async waiter class
 */
public abstract class BaseWaiterClassSpec implements ClassSpec {

    private static final String WAITERS_USER_AGENT = "waiter";
    private final IntermediateModel model;
    private final String modelPackage;
    private final Map<String, WaiterDefinition> waiters;
    private final ClassName waiterClassName;
    private final JmesPathAcceptorGenerator jmesPathAcceptorGenerator;
    private final PoetExtensions poetExtensions;

    public BaseWaiterClassSpec(IntermediateModel model, ClassName waiterClassName) {
        this.model = model;
        this.modelPackage = model.getMetadata().getFullModelPackageName();
        this.waiters = model.getWaiters();
        this.waiterClassName = waiterClassName;
        this.jmesPathAcceptorGenerator = new JmesPathAcceptorGenerator(waitersRuntimeClass());
        this.poetExtensions = new PoetExtensions(model);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder typeSpecBuilder = PoetUtils.createClassBuilder(className());
        typeSpecBuilder.addAnnotation(SdkInternalApi.class);
        typeSpecBuilder.addAnnotation(ThreadSafe.class);
        typeSpecBuilder.addModifiers(FINAL);
        typeSpecBuilder.addSuperinterface(interfaceClassName());
        typeSpecBuilder.addMethod(constructor());
        typeSpecBuilder.addField(FieldSpec.builder(ParameterizedTypeName.get(WaiterAttribute.class, SdkAutoCloseable.class),
                                                   "CLIENT_ATTRIBUTE", PRIVATE, STATIC, FINAL)
                                          .initializer("new $T<>($T.class)", WaiterAttribute.class, SdkAutoCloseable.class)
                                          .build());
        typeSpecBuilder.addField(clientClassName(), "client", PRIVATE, FINAL);
        typeSpecBuilder.addField(ClassName.get(AttributeMap.class), "managedResources", PRIVATE, FINAL);
        typeSpecBuilder.addMethod(staticErrorCodeMethod());
        typeSpecBuilder.addMethods(waiterOperations());
        typeSpecBuilder.addMethods(waiterAcceptorInitializers());
        typeSpecBuilder.addMethods(waiterConfigInitializers());
        typeSpecBuilder.addFields(waitersFields());
        additionalTypeSpecModification(typeSpecBuilder);

        typeSpecBuilder.addMethod(closeMethod());

        typeSpecBuilder.addMethod(MethodSpec.methodBuilder("builder")
                                            .addModifiers(Modifier.PUBLIC, STATIC)
                                            .returns(interfaceClassName().nestedClass("Builder"))
                                            .addStatement("return new DefaultBuilder()")
                                            .build());

        typeSpecBuilder.addType(builder());
        typeSpecBuilder.addMethod(applyWaitersUserAgentMethod(poetExtensions, model));
        return typeSpecBuilder.build();
    }

    private MethodSpec closeMethod() {
        return MethodSpec.methodBuilder("close")
                         .addAnnotation(Override.class)
                         .addModifiers(PUBLIC)
                         .addStatement("managedResources.close()")
                         .build();
    }

    protected abstract ClassName clientClassName();

    protected abstract TypeName getWaiterResponseType(OperationModel opModel);

    protected abstract ClassName interfaceClassName();

    protected void additionalTypeSpecModification(TypeSpec.Builder type) {
        // no-op
    }

    protected void additionalConstructorInitialization(MethodSpec.Builder method) {
        // no-op
    }

    protected void additionalBuilderTypeSpecModification(TypeSpec.Builder builder) {
        // no-op
    }

    protected Optional<String> additionalWaiterConfig() {
        return Optional.empty();
    }

    private MethodSpec constructor() {
        MethodSpec.Builder ctor = MethodSpec.constructorBuilder()
                                            .addModifiers(PRIVATE)
                                            .addParameter(className().nestedClass("DefaultBuilder"), "builder");
        ctor.addStatement("$T attributeMapBuilder = $T.builder()", ClassName.get(AttributeMap.class).nestedClass("Builder"),
                          AttributeMap.class);
        ctor.beginControlFlow("if (builder.client == null)")
            .addStatement("this.client = $T.builder().build()", clientClassName())
            .addStatement("attributeMapBuilder.put(CLIENT_ATTRIBUTE, this.client)")
            .endControlFlow();
        ctor.beginControlFlow("else")
            .addStatement("this.client = builder.client")
            .endControlFlow();

        additionalConstructorInitialization(ctor);

        ctor.addStatement("managedResources = attributeMapBuilder.build()");

        waiters.entrySet().stream()
               .map(this::waiterFieldInitialization)
               .forEach(ctor::addCode);

        return ctor.build();
    }

    private List<MethodSpec> waiterConfigInitializers() {
        List<MethodSpec> initializers = new ArrayList<>();
        waiters.forEach((k, v) -> initializers.add(waiterConfigInitializer(k, v)));
        return initializers;
    }

    private MethodSpec waiterConfigInitializer(String waiterKey, WaiterDefinition waiterDefinition) {
        ClassName overrideConfig = ClassName.get(WaiterOverrideConfiguration.class);
        MethodSpec.Builder configMethod =
            MethodSpec.methodBuilder(waiterFieldName(waiterKey) + "Config")
                      .addModifiers(PRIVATE, STATIC)
                      .addParameter(overrideConfig, "overrideConfig")
                      .returns(overrideConfig);

        configMethod.addStatement("$T<$T> optionalOverrideConfig = Optional.ofNullable(overrideConfig)",
                                  Optional.class,
                                  WaiterOverrideConfiguration.class);
        configMethod.addStatement("int maxAttempts = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::maxAttempts)"
                                  + ".orElse($L)",
                                  waiterDefinition.getMaxAttempts());
        configMethod.addStatement("$T backoffStrategy = optionalOverrideConfig."
                                  + "flatMap(WaiterOverrideConfiguration::backoffStrategy).orElse($T.create($T.ofSeconds($L)))",
                                  BackoffStrategy.class,
                                  FixedDelayBackoffStrategy.class,
                                  Duration.class,
                                  waiterDefinition.getDelay());
        configMethod.addStatement("$T waitTimeout = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::waitTimeout)"
                                  + ".orElse(null)",
                                  Duration.class);

        configMethod.addStatement("return WaiterOverrideConfiguration.builder().maxAttempts(maxAttempts).backoffStrategy"
                                  + "(backoffStrategy).waitTimeout(waitTimeout).build()");
        return configMethod.build();
    }

    private CodeBlock waiterFieldInitialization(Map.Entry<String, WaiterDefinition> waiterDefinition) {
        String waiterKey = waiterDefinition.getKey();
        WaiterDefinition waiter = waiterDefinition.getValue();
        OperationModel opModel = operationModel(waiter);
        CodeBlock.Builder codeBlockBuilder = CodeBlock
            .builder();

        String waiterFieldName = waiterFieldName(waiterKey);
        codeBlockBuilder.add("this.$L = $T.builder($T.class)"
                             + ".acceptors($LAcceptors()).overrideConfiguration($LConfig(builder.overrideConfiguration))",
                             waiterFieldName,
                             waiterClassName,
                             ClassName.get(modelPackage, opModel.getReturnType().getReturnType()),
                             waiterFieldName,
                             waiterFieldName);

        additionalWaiterConfig().ifPresent(codeBlockBuilder::add);
        codeBlockBuilder.addStatement(".build()");
        return codeBlockBuilder.build();
    }

    private List<FieldSpec> waitersFields() {
        return waiters.entrySet().stream()
                      .map(this::waiterField)
                      .collect(Collectors.toList());
    }

    private FieldSpec waiterField(Map.Entry<String, WaiterDefinition> waiterDefinition) {
        OperationModel opModel = operationModel(waiterDefinition.getValue());
        ClassName pojoResponse = ClassName.get(modelPackage, opModel.getReturnType().getReturnType());
        String fieldName = waiterFieldName(waiterDefinition.getKey());
        return FieldSpec.builder(ParameterizedTypeName.get(waiterClassName,
                                                           pojoResponse), fieldName)
                        .addModifiers(PRIVATE, FINAL)
                        .build();
    }

    private TypeSpec builder() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("DefaultBuilder")
                                           .addModifiers(PUBLIC, STATIC, FINAL)
                                           .addSuperinterface(interfaceClassName().nestedClass("Builder"))
                                           .addField(clientClassName(), "client", PRIVATE)
                                           .addField(ClassName.get(WaiterOverrideConfiguration.class),
                                                     "overrideConfiguration", PRIVATE);

        additionalBuilderTypeSpecModification(builder);
        builder.addMethods(builderMethods());
        builder.addMethod(MethodSpec.constructorBuilder()
                                    .addModifiers(PRIVATE)
                                    .build());
        return builder.build();
    }

    private List<MethodSpec> builderMethods() {
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(MethodSpec.methodBuilder("overrideConfiguration")
                              .addModifiers(Modifier.PUBLIC)
                              .addAnnotation(Override.class)
                              .addParameter(ClassName.get(WaiterOverrideConfiguration.class), "overrideConfiguration")
                              .addStatement("this.overrideConfiguration = overrideConfiguration")
                              .addStatement("return this")
                              .returns(interfaceClassName().nestedClass("Builder"))
                              .build());
        methods.add(MethodSpec.methodBuilder("client")
                              .addModifiers(Modifier.PUBLIC)
                              .addAnnotation(Override.class)
                              .addParameter(clientClassName(), "client")
                              .addStatement("this.client = client")
                              .addStatement("return this")
                              .returns(interfaceClassName().nestedClass("Builder"))
                              .build());
        methods.add(MethodSpec.methodBuilder("build")
                              .addModifiers(Modifier.PUBLIC)
                              .returns(interfaceClassName())
                              .addStatement("return new $T(this)", className())
                              .build());
        return methods;

    }

    private List<MethodSpec> waiterOperations() {
        return waiters.entrySet()
                      .stream()
                      .flatMap(this::waiterOperations)
                      .sorted(Comparator.comparing(m -> m.name))
                      .collect(Collectors.toList());
    }

    private Stream<MethodSpec> waiterOperations(Map.Entry<String, WaiterDefinition> waiterDefinition) {
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(waiterOperation(waiterDefinition));
        methods.add(waiterOperationWithOverrideConfig(waiterDefinition));
        return methods.stream();
    }

    private MethodSpec waiterOperationWithOverrideConfig(Map.Entry<String, WaiterDefinition> waiterDefinition) {
        String waiterMethodName = waiterDefinition.getKey();
        OperationModel opModel = operationModel(waiterDefinition.getValue());

        ClassName overrideConfig = ClassName.get(WaiterOverrideConfiguration.class);
        ClassName requestType = ClassName.get(modelPackage, opModel.getInput().getVariableType());

        String waiterFieldName = waiterFieldName(waiterDefinition.getKey());
        MethodSpec.Builder builder = methodSignatureWithReturnType(waiterMethodName, opModel)
            .addParameter(requestType, opModel.getInput().getVariableName())
            .addParameter(overrideConfig, "overrideConfig")
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .addStatement("return $L.$L(() -> client.$N(applyWaitersUserAgent($N)), $LConfig(overrideConfig))",
                          waiterFieldName,
                          waiterClassName.simpleName().equals("Waiter") ? "run" : "runAsync",
                          lowercaseFirstChar(waiterDefinition.getValue().getOperation()),
                          opModel.getInput().getVariableName(),
                          waiterFieldName);

        return builder.build();
    }

    private MethodSpec waiterOperation(Map.Entry<String, WaiterDefinition> waiterDefinition) {
        String waiterMethodName = waiterDefinition.getKey();
        OperationModel opModel = operationModel(waiterDefinition.getValue());

        ClassName requestType = ClassName.get(modelPackage, opModel.getInput().getVariableType());

        MethodSpec.Builder builder = methodSignatureWithReturnType(waiterMethodName, opModel)
            .addParameter(requestType, opModel.getInput().getVariableName())
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .addStatement("return $L.$L(() -> client.$N(applyWaitersUserAgent($N)))",
                          waiterFieldName(waiterMethodName),
                          waiterClassName.simpleName().equals("Waiter") ? "run" : "runAsync",
                          lowercaseFirstChar(waiterDefinition.getValue().getOperation()),
                          opModel.getInput().getVariableName());

        return builder.build();
    }

    private List<MethodSpec> waiterAcceptorInitializers() {
        List<MethodSpec> initializers = new ArrayList<>();
        waiters.forEach((k, v) -> initializers.add(acceptorInitializer(k, v)));
        return initializers;
    }

    private MethodSpec acceptorInitializer(String waiterKey, WaiterDefinition waiterDefinition) {
        MethodSpec.Builder acceptorsMethod =
            MethodSpec.methodBuilder(waiterFieldName(waiterKey) + "Acceptors")
                      .addModifiers(PRIVATE, STATIC)
                      .returns(waiterAcceptorTypeName(waiterDefinition));

        acceptorsMethod.addStatement("$T result = new $T<>()", waiterAcceptorTypeName(waiterDefinition), ArrayList.class);

        for (Acceptor acceptor : waiterDefinition.getAcceptors()) {
            acceptorsMethod.addCode("result.add(")
                           .addCode(acceptor(acceptor))
                           .addCode(");");
        }

        acceptorsMethod.addStatement("result.addAll($T.DEFAULT_ACCEPTORS)", waitersRuntimeClass());

        acceptorsMethod.addStatement("return result");

        return acceptorsMethod.build();
    }

    protected String waiterFieldName(String waiterKey) {
        return lowercaseFirstChar(waiterKey) + "Waiter";
    }

    private OperationModel operationModel(WaiterDefinition waiterDefinition) {
        return model.getOperation(waiterDefinition.getOperation());
    }

    private MethodSpec.Builder methodSignatureWithReturnType(String waiterMethodName, OperationModel opModel) {
        return MethodSpec.methodBuilder(getWaiterMethodName(waiterMethodName))
                         .returns(getWaiterResponseType(opModel));
    }

    static MethodSpec applyWaitersUserAgentMethod(PoetExtensions poetExtensions, IntermediateModel model) {

        TypeVariableName typeVariableName =
            TypeVariableName.get("T", poetExtensions.getModelClass(model.getSdkRequestBaseClassName()));

        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName
            .get(ClassName.get(Consumer.class), ClassName.get(AwsRequestOverrideConfiguration.Builder.class));

        CodeBlock codeBlock = CodeBlock.builder()
                                       .addStatement("$T userAgentApplier = b -> b.addApiName($T.builder().version"
                                                     + "($S).name($S).build())",
                                                     parameterizedTypeName, ApiName.class,
                                                     WAITERS_USER_AGENT,
                                                     "hll")
                                       .addStatement("$T overrideConfiguration =\n"
                                                     + "            request.overrideConfiguration().map(c -> c.toBuilder()"
                                                     + ".applyMutation"
                                                     + "(userAgentApplier).build())\n"
                                                     + "            .orElse((AwsRequestOverrideConfiguration.builder()"
                                                     + ".applyMutation"
                                                     + "(userAgentApplier).build()))", AwsRequestOverrideConfiguration.class)
                                       .addStatement("return (T) request.toBuilder().overrideConfiguration"
                                                     + "(overrideConfiguration).build()")
                                       .build();

        return MethodSpec.methodBuilder("applyWaitersUserAgent")
                         .addModifiers(Modifier.PRIVATE)
                         .addParameter(typeVariableName, "request")
                         .addTypeVariable(typeVariableName)
                         .addCode(codeBlock)
                         .returns(typeVariableName)
                         .build();
    }

    private String getWaiterMethodName(String waiterMethodName) {
        return "waitUntil" + waiterMethodName;
    }

    private TypeName waiterAcceptorTypeName(WaiterDefinition waiterDefinition) {
        WildcardTypeName wildcardTypeName = WildcardTypeName.supertypeOf(fullyQualifiedResponseType(waiterDefinition));

        return ParameterizedTypeName.get(ClassName.get(List.class),
                                         ParameterizedTypeName.get(ClassName.get(WaiterAcceptor.class), wildcardTypeName));
    }

    private TypeName fullyQualifiedResponseType(WaiterDefinition waiterDefinition) {
        String modelPackage = model.getMetadata().getFullModelPackageName();
        String operationResponseType = model.getOperation(waiterDefinition.getOperation()).getReturnType().getReturnType();
        return ClassName.get(modelPackage, operationResponseType);
    }

    private CodeBlock acceptor(Acceptor acceptor) {
        CodeBlock.Builder result = CodeBlock.builder();

        switch (acceptor.getState()) {
            case "success":
                result.add("$T.success", WaiterAcceptor.class);
                break;
            case "failure":
                result.add("$T.error", WaiterAcceptor.class);
                break;
            case "retry":
                result.add("$T.retry", WaiterAcceptor.class);
                break;
            default:
                throw new IllegalArgumentException("Unsupported acceptor state: " + acceptor.getState());
        }

        switch (acceptor.getMatcher()) {
            case "path":
                result.add("OnResponseAcceptor(");
                result.add(pathAcceptorBody(acceptor));
                result.add(")");
                break;
            case "pathAll":
                result.add("OnResponseAcceptor(");
                result.add(pathAllAcceptorBody(acceptor));
                result.add(")");
                break;
            case "pathAny":
                result.add("OnResponseAcceptor(");
                result.add(pathAnyAcceptorBody(acceptor));
                result.add(")");
                break;
            case "status":
                // Note: Ignores the result we've built so far because this uses a special acceptor implementation.
                int expected = Integer.parseInt(acceptor.getExpected().asText());
                return CodeBlock.of("new $T($L, $T.$L)", waitersRuntimeClass().nestedClass("ResponseStatusAcceptor"),
                                    expected, WaiterState.class, waiterState(acceptor));
            case "error":
                result.add("OnExceptionAcceptor(");
                result.add(errorAcceptorBody(acceptor));
                result.add(")");
                break;
            default:
                throw new IllegalArgumentException("Unsupported acceptor matcher: " + acceptor.getMatcher());
        }

        return result.build();
    }

    private String waiterState(Acceptor acceptor) {
        switch (acceptor.getState()) {
            case "success":
                return WaiterState.SUCCESS.name();
            case "failure":
                return WaiterState.FAILURE.name();
            case "retry":
                return WaiterState.RETRY.name();
            default:
                throw new IllegalArgumentException("Unsupported acceptor state: " + acceptor.getState());
        }
    }

    private CodeBlock pathAcceptorBody(Acceptor acceptor) {
        String expected = acceptor.getExpected().asText();
        String expectedType = acceptor.getExpected() instanceof JrsString ? "$S" : "$L";
        return CodeBlock.builder()
                        .add("response -> {")
                        .add("$1T input = new $1T(response);", waitersRuntimeClass().nestedClass("Value"))
                        .add("return $T.equals(", Objects.class)
                        .add(jmesPathAcceptorGenerator.interpret(acceptor.getArgument(), "input"))
                        .add(".value(), " + expectedType + ");", expected)
                        .add("}")
                        .build();
    }

    private CodeBlock pathAllAcceptorBody(Acceptor acceptor) {
        String expected = acceptor.getExpected().asText();
        String expectedType = acceptor.getExpected() instanceof JrsString ? "$S" : "$L";
        return CodeBlock.builder()
                        .add("response -> {")
                        .add("$1T input = new $1T(response);", waitersRuntimeClass().nestedClass("Value"))
                        .add("$T<$T> resultValues = ", List.class, Object.class)
                        .add(jmesPathAcceptorGenerator.interpret(acceptor.getArgument(), "input"))
                        .add(".values();")
                        .add("return !resultValues.isEmpty() && "
                             + "resultValues.stream().allMatch(v -> $T.equals(v, " + expectedType + "));",
                             Objects.class, expected)
                        .add("}")
                        .build();
    }

    private CodeBlock pathAnyAcceptorBody(Acceptor acceptor) {
        String expected = acceptor.getExpected().asText();
        String expectedType = acceptor.getExpected() instanceof JrsString ? "$S" : "$L";
        return CodeBlock.builder()
                        .add("response -> {")
                        .add("$1T input = new $1T(response);", waitersRuntimeClass().nestedClass("Value"))
                        .add("$T<$T> resultValues = ", List.class, Object.class)
                        .add(jmesPathAcceptorGenerator.interpret(acceptor.getArgument(), "input"))
                        .add(".values();")
                        .add("return !resultValues.isEmpty() && "
                             + "resultValues.stream().anyMatch(v -> $T.equals(v, " + expectedType + "));",
                             Objects.class, expected)
                        .add("}")
                        .build();
    }

    private CodeBlock errorAcceptorBody(Acceptor acceptor) {
        String expected = acceptor.getExpected().asText();
        String expectedType = acceptor.getExpected() instanceof JrsString ? "$S" : "$L";
        return CodeBlock.of("error -> $T.equals(errorCode(error), " + expectedType + ")", Objects.class, expected);
    }

    private MethodSpec staticErrorCodeMethod() {
        return MethodSpec.methodBuilder("errorCode")
                         .addModifiers(PRIVATE, STATIC)
                         .returns(String.class)
                         .addParameter(Throwable.class, "error")
                         .addCode("if (error instanceof $T) {", AwsServiceException.class)
                         .addCode("return (($T) error).awsErrorDetails().errorCode();", AwsServiceException.class)
                         .addCode("}")
                         .addCode("return null;")
                         .build();
    }

    private ClassName waitersRuntimeClass() {
        return ClassName.get(model.getMetadata().getFullWaitersInternalPackageName(),
                             WaitersRuntimeGeneratorTask.RUNTIME_CLASS_NAME);
    }
}
