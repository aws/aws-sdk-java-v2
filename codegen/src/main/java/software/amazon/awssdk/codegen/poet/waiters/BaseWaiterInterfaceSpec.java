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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.docs.WaiterDocs;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.WaiterDefinition;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.waiters.PollingStrategy;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Base class contains shared logic used in both sync waiter and async waiter interfaces.
 */
public abstract class BaseWaiterInterfaceSpec implements ClassSpec {

    private final IntermediateModel model;
    private final Map<String, WaiterDefinition> waiters;
    private final String modelPackage;

    public BaseWaiterInterfaceSpec(IntermediateModel model) {
        this.modelPackage = model.getMetadata().getFullModelPackageName();
        this.model = model;
        this.waiters = model.getWaiters();
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder result = PoetUtils.createInterfaceBuilder(className());
        result.addAnnotation(SdkPublicApi.class);
        result.addMethods(waiterOperations());
        result.addSuperinterface(SdkAutoCloseable.class);
        result.addMethod(MethodSpec.methodBuilder("builder")
                                   .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                   .addJavadoc(WaiterDocs.waiterBuilderMethodJavadoc(className()))
                                   .returns(className().nestedClass("Builder"))
                                   .addStatement("return $T.builder()", waiterImplName())
                                   .build());
        result.addJavadoc(WaiterDocs.waiterInterfaceJavadoc());

        result.addType(builderInterface());
        return result.build();
    }

    protected abstract ClassName waiterImplName();

    protected abstract ClassName clientClassName();

    protected abstract ParameterizedTypeName getWaiterResponseType(OperationModel operationModel);

    protected void additionalBuilderTypeSpecModification(TypeSpec.Builder type) {
        // no-op
    }

    /**
     * @return List generated of traditional (request/response) methods for all operations.
     */
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
        return methods.stream();
    }

    private MethodSpec waiterOperation(Map.Entry<String, WaiterDefinition> waiterDefinition) {
        String waiterMethodName = waiterDefinition.getKey();
        OperationModel opModel = model.getOperation(waiterDefinition.getValue().getOperation());

        ClassName requestType = ClassName.get(modelPackage, opModel.getInput().getVariableType());
        CodeBlock javadoc = WaiterDocs.waiterOperationJavadoc(clientClassName(), waiterDefinition, opModel);

        MethodSpec.Builder builder = methodSignatureWithReturnType(waiterMethodName, opModel)
            .addParameter(requestType, opModel.getInput().getVariableName())
            .addJavadoc(javadoc);
        return unsupportedOperation(builder).build();
    }

    private MethodSpec.Builder methodSignatureWithReturnType(String waiterMethodName, OperationModel opModel) {
        return MethodSpec.methodBuilder(getWaiterMethodName(waiterMethodName))
                         .returns(getWaiterResponseType(opModel));
    }

    private String getWaiterMethodName(String waiterMethodName) {
        return "waitUntil" + waiterMethodName;
    }

    private MethodSpec.Builder unsupportedOperation(MethodSpec.Builder builder) {
        return builder.addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                      .addStatement("throw new $T()", UnsupportedOperationException.class);
    }

    private TypeSpec builderInterface() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("Builder")
                                           .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        additionalBuilderTypeSpecModification(builder);
        builder.addMethods(builderMethods());
        return builder.build();
    }

    private List<MethodSpec> builderMethods() {
        List<MethodSpec> builderMethods = new ArrayList<>();
        builderMethods.add(MethodSpec.methodBuilder("pollingStrategy")
                                     .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                     .addParameter(ClassName.get(PollingStrategy.class), "pollingStrategy")
                                     .addJavadoc(WaiterDocs.waiterBuilderPollingStrategy())
                                     .returns(className().nestedClass("Builder"))
                                     .build());
        builderMethods.add(MethodSpec.methodBuilder("client")
                                     .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                     .addParameter(clientClassName(), "client")
                                     .addJavadoc(WaiterDocs.waiterBuilderClientJavadoc(clientClassName()))
                                     .returns(className().nestedClass("Builder"))
                                     .build());
        builderMethods.add(MethodSpec.methodBuilder("build")
                                     .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                     .addJavadoc(WaiterDocs.waiterBuilderBuildJavadoc(className()))
                                     .returns(className())
                                     .build());
        return builderMethods;
    }
}
