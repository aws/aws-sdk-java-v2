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

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static software.amazon.awssdk.codegen.docs.BatchManagerDocs.batchManagerBuilderClientJavadoc;
import static software.amazon.awssdk.codegen.docs.BatchManagerDocs.batchManagerBuilderMethodJavadoc;
import static software.amazon.awssdk.codegen.docs.BatchManagerDocs.batchManagerBuilderPollingStrategy;
import static software.amazon.awssdk.codegen.docs.BatchManagerDocs.batchManagerBuilderScheduledExecutorServiceJavadoc;
import static software.amazon.awssdk.codegen.docs.BatchManagerDocs.batchManagerSyncInterfaceDocs;
import static software.amazon.awssdk.codegen.docs.BatchManagerDocs.batchMethodDocs;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getRequestType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getResponseType;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.docs.WaiterDocs;
import software.amazon.awssdk.codegen.model.config.customization.BatchManagerMethods;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.utils.SdkAutoCloseable;

public abstract class BaseBatchManagerInterfaceSpec implements ClassSpec {

    private final IntermediateModel model;
    private final String modelPackage;
    private final String serviceName;
    private final PoetExtensions poetExtensions;
    private final Map<String, BatchManagerMethods> batchFunctions;

    protected BaseBatchManagerInterfaceSpec(IntermediateModel model) {
        this.model = model;
        this.modelPackage = model.getMetadata().getFullModelPackageName();
        this.serviceName = model.getMetadata().getServiceName();
        this.poetExtensions = new PoetExtensions(model);
        this.batchFunctions = model.getCustomizationConfig().getBatchManagerMethods();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createInterfaceBuilder(className())
                                            .addAnnotation(SdkPublicApi.class)
                                            .addSuperinterface(SdkAutoCloseable.class);
        builder.addMethods(batchFunctions());
        builder.addMethod(MethodSpec.methodBuilder("builder")
                                    .addModifiers(PUBLIC, STATIC)
                                    .addJavadoc(batchManagerBuilderMethodJavadoc(className()))
                                    .returns(className().nestedClass("Builder"))
                                    .addStatement("return $T.builder()", defaultBatchManagerName())
                                    .build());
        builder.addType(builderInterface());
        builder.addJavadoc(batchManagerSyncInterfaceDocs(serviceName, isSync()));
        return builder.build();
    }

    private List<MethodSpec> batchFunctions() {
        return batchFunctions.entrySet()
                             .stream()
                             .map(this::batchFunctions)
                             .collect(Collectors.toList());
    }

    private MethodSpec batchFunctions(Map.Entry<String, BatchManagerMethods> batchFunctions) {
        String methodName = batchFunctions.getKey();
        ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(CompletableFuture.class),
                                                                     getResponseType(batchFunctions, modelPackage));
        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, returnType)
            .addParameter(getRequestType(batchFunctions, modelPackage), "request")
            .addJavadoc(batchMethodDocs(serviceName, batchFunctions, modelPackage));
        return unsupportedOperation(builder).build();
    }

    private TypeSpec builderInterface() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("Builder");
        additionalBuilderTypeSpecModification(builder);
        builder.addMethods(builderMethods())
               .addModifiers(PUBLIC, STATIC);
        return builder.build();
    }

    private List<MethodSpec> builderMethods() {
        ClassName clientName = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
        List<MethodSpec> builderMethods = new ArrayList<>();
        builderMethods.add(MethodSpec.methodBuilder("overrideConfiguration")
                                     .addModifiers(PUBLIC, ABSTRACT)
                                     .addParameter(ClassName.get(BatchOverrideConfiguration.class), "overrideConfiguration")
                                     .addJavadoc(batchManagerBuilderPollingStrategy())
                                     .returns(className().nestedClass("Builder"))
                                     .build());
        builderMethods.add(MethodSpec.methodBuilder("client")
                                     .addModifiers(PUBLIC, ABSTRACT)
                                     .addParameter(clientClassName(), "client")
                                     .addJavadoc(batchManagerBuilderClientJavadoc(clientName))
                                     .returns(className().nestedClass("Builder"))
                                     .build());
        builderMethods.add(MethodSpec.methodBuilder("scheduledExecutor")
                                     .addModifiers(PUBLIC, ABSTRACT)
                                     .addParameter(ClassName.get(ScheduledExecutorService.class), "scheduledExecutor")
                                     .addJavadoc(batchManagerBuilderScheduledExecutorServiceJavadoc(className().simpleName()))
                                     .returns(className().nestedClass("Builder"))
                                     .build());
        builderMethods.add(MethodSpec.methodBuilder("build")
                                     .addModifiers(PUBLIC, ABSTRACT)
                                     .addJavadoc(WaiterDocs.waiterBuilderBuildJavadoc(className()))
                                     .returns(className())
                                     .build());
        return builderMethods;
    }

    private MethodSpec.Builder unsupportedOperation(MethodSpec.Builder builder) {
        return builder.addModifiers(DEFAULT, PUBLIC)
                      .addStatement("throw new $T()", UnsupportedOperationException.class);
    }

    private MethodSpec.Builder methodSignatureWithReturnType(String methodName, TypeName returnType) {
        return MethodSpec.methodBuilder(methodName)
                         .returns(returnType);
    }

    protected abstract ClassName defaultBatchManagerName();

    protected abstract ClassName clientClassName();

    protected abstract boolean isSync();

    protected void additionalBuilderTypeSpecModification(TypeSpec.Builder type) {
        // no-op
    }

}
