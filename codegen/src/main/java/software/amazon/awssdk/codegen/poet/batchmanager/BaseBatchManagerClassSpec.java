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

package software.amazon.awssdk.codegen.poet.batchmanager;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getBatchResponseType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getRequestType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getResponseType;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.BatchManagerMethods;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.batchmanager.BatchManager;
import software.amazon.awssdk.core.batchmanager.BatchOverrideConfiguration;

public abstract class BaseBatchManagerClassSpec implements ClassSpec {

    private final String modelPackage;
    private final PoetExtensions poetExtensions;
    private final Map<String, BatchManagerMethods> batchFunctions;

    protected BaseBatchManagerClassSpec(IntermediateModel model) {
        this.modelPackage = model.getMetadata().getFullModelPackageName();
        this.poetExtensions = new PoetExtensions(model);
        this.batchFunctions = model.getCustomizationConfig().getBatchManagerMethods();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder classBuilder = PoetUtils.createClassBuilder(className())
                                                 .addAnnotation(SdkInternalApi.class)
                                                 .addModifiers(PUBLIC, FINAL)
                                                 .addSuperinterface(interfaceClassName());

        ClassName batchManagerType = ClassName.get(BatchManager.class);
        additionalTypeSpecModification(classBuilder);
        classBuilder.addField(clientClassName(), "client", PRIVATE, FINAL);
        batchFunctions.entrySet()
                      .forEach(batchFunctionsEntry -> {
                          String batchManagerParam = batchFunctionsEntry.getKey() + "BatchManager";
                          ClassName requestType = getRequestType(batchFunctionsEntry, modelPackage);
                          ClassName responseType = getResponseType(batchFunctionsEntry, modelPackage);
                          ClassName batchResponseType = getBatchResponseType(batchFunctionsEntry, modelPackage);

                          classBuilder.addField(FieldSpec.builder(ParameterizedTypeName.get(batchManagerType, requestType,
                                                                                            responseType, batchResponseType),
                                                                  batchManagerParam, PRIVATE, FINAL)
                                                         .build());
                      });

        classBuilder.addMethod(constructor())
                    .addMethod(internalTestConstructor())
                    .addMethods(batchManagerMethods())
                    .addMethods(configMethods())
                    .addMethod(close())
                    .addMethod(MethodSpec.methodBuilder("builder")
                                         .addModifiers(PUBLIC, STATIC)
                                         .returns(interfaceClassName().nestedClass("Builder"))
                                         .addStatement("return new DefaultBuilder()")
                                         .build());
        additionalExecutorInitialization(classBuilder);

        classBuilder.addType(builder());

        return classBuilder.build();
    }

    private MethodSpec constructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                                               .addModifiers(PRIVATE)
                                               .addParameter(className().nestedClass("DefaultBuilder"), "builder");
        builder.addStatement("this.client = builder.client")
               .addStatement("$T scheduledExecutor = builder.scheduledExecutor", ClassName.get(ScheduledExecutorService.class));

        // TODO: Figure out how to properly create and pass service configurations. Should it be the same configuration for each
        //  batchManager (so out here) or a separate configuration for each batchManager (so in the forEach loop).

        additionalConstructorInitialization(builder);
        ClassName batchFunctionsClass = poetExtensions.getBatchFunctionsClass();
        batchFunctions.entrySet()
                      .forEach(batchFunctionsEntry -> {
                          String batchManagerParam = batchFunctionsEntry.getKey() + "BatchManager";
                          String requestMethod = batchFunctionsEntry.getKey();
                          ClassName requestType = getRequestType(batchFunctionsEntry, modelPackage);
                          ClassName responseType = getResponseType(batchFunctionsEntry, modelPackage);
                          ClassName batchResponseType = getBatchResponseType(batchFunctionsEntry, modelPackage);

                          builder.addCode("this.$L = $T.builder($T.class, $T.class, $T.class)\n",
                                          batchManagerParam, ClassName.get(BatchManager.class), requestType,
                                          responseType, batchResponseType);

                          if (isSync()) {
                              builder.addCode(".batchFunction($T.$L(client, executor))\n", batchFunctionsClass,
                                              requestMethod + "BatchFunction");
                          } else {
                              builder.addCode(".batchFunction($T.$L(client))\n", batchFunctionsClass,
                                              requestMethod + "BatchAsyncFunction");
                          }
                          builder.addStatement(".responseMapper($T.$L())\n"
                                               + ".batchKeyMapper($T.$L())\n"
                                               + ".overrideConfiguration($N(builder.overrideConfiguration))\n"
                                               + ".scheduledExecutor(scheduledExecutor)\n"
                                               + ".build()",
                                               batchFunctionsClass, requestMethod + "ResponseMapper",
                                               batchFunctionsClass, requestMethod + "BatchKeyMapper",
                                               configMethod(batchFunctionsEntry));

                      });

        return builder.build();
    }

    private MethodSpec internalTestConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                                               .addModifiers(PUBLIC)
                                               .addAnnotation(SdkInternalApi.class);

        ClassName batchManagerType = ClassName.get(BatchManager.class);
        builder.addParameter(clientClassName(), "client");
        batchFunctions.entrySet()
                      .forEach(batchFunctionsEntry -> {
                          String batchManagerParam = batchFunctionsEntry.getKey() + "BatchManager";
                          ClassName requestType = getRequestType(batchFunctionsEntry, modelPackage);
                          ClassName responseType = getResponseType(batchFunctionsEntry, modelPackage);
                          ClassName batchResponseType = getBatchResponseType(batchFunctionsEntry, modelPackage);

                          builder.addParameter(ParameterizedTypeName.get(batchManagerType, requestType,
                                                                         responseType, batchResponseType),
                                               batchManagerParam)
                                 .addStatement("this.$L = $L", batchManagerParam, batchManagerParam);
                      });
        builder.addStatement("this.client = client");
        additionalTestConstructorInitialization(builder);

        return builder.build();
    }

    private List<MethodSpec> batchManagerMethods() {
        return batchFunctions.entrySet()
                             .stream()
                             .map(this::batchManagerMethod)
                             .collect(Collectors.toList());
    }

    private MethodSpec batchManagerMethod(Map.Entry<String, BatchManagerMethods> batchManagerMethod) {
        String methodName = batchManagerMethod.getKey();
        String batchManagerName = methodName + "BatchManager";
        ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(CompletableFuture.class),
                                                                     getResponseType(batchManagerMethod, modelPackage));
        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, returnType)
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .addParameter(getRequestType(batchManagerMethod, modelPackage), "request")
            .addStatement("return $L.sendRequest(request)", batchManagerName);

        return builder.build();
    }

    private List<MethodSpec> configMethods() {
        return batchFunctions.entrySet()
                             .stream()
                             .map(this::configMethod)
                             .collect(Collectors.toList());
    }

    private MethodSpec configMethod(Map.Entry<String, BatchManagerMethods> batchManagerMethod) {
        String methodName = batchManagerMethod.getKey() + "Config";
        BatchManagerMethods batchManager = batchManagerMethod.getValue();
        ClassName batchOverrideConfigClass = ClassName.get(BatchOverrideConfiguration.class);
        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, batchOverrideConfigClass)
            .addModifiers(PRIVATE)
            .addParameter(batchOverrideConfigClass, "overrideConfiguration");

        builder.addStatement("$T.Builder config = $T.builder()", batchOverrideConfigClass, batchOverrideConfigClass)
               .beginControlFlow("if (overrideConfiguration == null)")
               .addStatement("config.maxBatchItems($L)", batchManager.getMaxBatchItems())
               .addStatement("config.maxBatchOpenInMs($T.ofMillis($L))", ClassName.get(Duration.class),
                             batchManager.getMaxBatchOpenInMs())
               .nextControlFlow("else")
               .addStatement("config.maxBatchItems(overrideConfiguration.maxBatchItems().orElse($L))",
                             batchManager.getMaxBatchItems())
               .addStatement("config.maxBatchOpenInMs(overrideConfiguration.maxBatchOpenInMs().orElse($T.ofMillis($L)))",
                             ClassName.get(Duration.class), batchManager.getMaxBatchOpenInMs())
               .endControlFlow()
               .addStatement("return config.build()");

        return builder.build();
    }

    private MethodSpec close() {
        MethodSpec.Builder builder = methodSignatureWithReturnType("close", TypeName.VOID)
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class);

        batchFunctions.keySet()
                      .forEach(requestMethod -> builder.addStatement("$L.close()", requestMethod + "BatchManager"));
        additionalCloseMethodModification(builder);

        return builder.build();
    }

    private TypeSpec builder() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("DefaultBuilder")
                                           .addModifiers(PUBLIC, STATIC, FINAL)
                                           .addSuperinterface(interfaceClassName().nestedClass("Builder"))
                                           .addField(clientClassName(), "client", PRIVATE)
                                           .addField(ClassName.get(BatchOverrideConfiguration.class),
                                                     "overrideConfiguration", PRIVATE)
                                           .addField(ClassName.get(ScheduledExecutorService.class), "scheduledExecutor", PRIVATE);

        builder.addMethod(MethodSpec.constructorBuilder()
                                    .addModifiers(PRIVATE)
                                    .build());
        additionalBuilderTypeSpecModification(builder);
        builder.addMethods(builderMethods());

        return builder.build();
    }

    private List<MethodSpec> builderMethods() {
        List<MethodSpec> methods = new ArrayList<>();
        MethodSpec.Builder overrideConfigMethod = MethodSpec.methodBuilder("overrideConfiguration")
                                                            .addParameter(ClassName.get(BatchOverrideConfiguration.class),
                                                                          "overrideConfiguration")
                                                            .addStatement("this.overrideConfiguration = overrideConfiguration");
        MethodSpec.Builder clientMethod = MethodSpec.methodBuilder("client")
                                                    .addParameter(clientClassName(), "client")
                                                    .addStatement("this.client = client");
        MethodSpec.Builder scheduledExecutorMethod = MethodSpec.methodBuilder("scheduledExecutor")
                                                               .addParameter(ClassName.get(ScheduledExecutorService.class),
                                                                             "scheduledExecutor")
                                                               .addStatement("this.scheduledExecutor = scheduledExecutor");
        methods.add(builderMethodCommonCode(overrideConfigMethod));
        methods.add(builderMethodCommonCode(clientMethod));
        methods.add(builderMethodCommonCode(scheduledExecutorMethod));
        methods.add(MethodSpec.methodBuilder("build")
                              .addModifiers(PUBLIC)
                              .returns(interfaceClassName())
                              .addStatement("return new $T(this)", className())
                              .build());
        return methods;
    }

    private MethodSpec builderMethodCommonCode(MethodSpec.Builder builder) {
        return builder.addModifiers(PUBLIC)
                      .addAnnotation(Override.class)
                      .addStatement("return this")
                      .returns(interfaceClassName().nestedClass("Builder"))
                      .build();
    }

    private MethodSpec.Builder methodSignatureWithReturnType(String methodName, TypeName returnType) {
        return MethodSpec.methodBuilder(methodName)
                         .returns(returnType);
    }

    protected abstract ClassName clientClassName();

    protected abstract ClassName interfaceClassName();

    protected abstract boolean isSync();

    protected void additionalTypeSpecModification(TypeSpec.Builder builder) {
        // no-op
    }

    protected void additionalConstructorInitialization(MethodSpec.Builder method) {
        // no-op
    }

    protected void additionalTestConstructorInitialization(MethodSpec.Builder method) {
        // no-op
    }

    protected void additionalBuilderTypeSpecModification(TypeSpec.Builder builder) {
        // no-op
    }

    protected void additionalCloseMethodModification(MethodSpec.Builder method) {
        // no-op
    }

    protected void additionalExecutorInitialization(TypeSpec.Builder method) {
        // no-op
    }
}
