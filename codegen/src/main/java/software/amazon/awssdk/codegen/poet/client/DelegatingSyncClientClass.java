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

package software.amazon.awssdk.codegen.poet.client;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static software.amazon.awssdk.codegen.poet.client.AsyncClientInterface.STREAMING_TYPE_VARIABLE;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.docs.SimpleMethodOverload;
import software.amazon.awssdk.codegen.model.config.customization.UtilitiesMethod;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.utils.Validate;

public class DelegatingSyncClientClass extends SyncClientInterface {
    private final IntermediateModel model;
    private final ClassName className;
    private final PoetExtension poetExtensions;

    public DelegatingSyncClientClass(IntermediateModel model) {
        super(model);
        this.model = model;
        this.className = ClassName.get(model.getMetadata().getFullClientPackageName(),
                                       "Delegating" + model.getMetadata().getSyncInterface());
        this.poetExtensions = new PoetExtension(model);
    }

    @Override
    protected void addInterfaceClass(TypeSpec.Builder type) {
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());

        type.addSuperinterface(interfaceClass)
            .addMethod(constructor(interfaceClass));
    }

    @Override
    protected TypeSpec.Builder createTypeSpec() {
        return PoetUtils.createClassBuilder(className);
    }

    @Override
    protected void addAnnotations(TypeSpec.Builder type) {
        type.addAnnotation(SdkPublicApi.class);
    }

    @Override
    protected void addModifiers(TypeSpec.Builder type) {
        type.addModifiers(ABSTRACT, PUBLIC);
    }

    @Override
    protected void addFields(TypeSpec.Builder type) {
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());

        type.addField(FieldSpec.builder(interfaceClass, "delegate")
                               .addModifiers(PRIVATE, FINAL)
                               .build());
    }

    @Override
    protected void addConsumerMethod(List<MethodSpec> specs, MethodSpec spec, SimpleMethodOverload overload,
                                     OperationModel opModel) {
    }

    @Override
    protected void addAdditionalMethods(TypeSpec.Builder type) {
        type.addMethod(nameMethod())
            .addMethod(delegateMethod())
            .addMethod(invokeMethod());
    }

    @Override
    protected void addCloseMethod(TypeSpec.Builder type) {
        MethodSpec method = MethodSpec.methodBuilder("close")
                                      .addAnnotation(Override.class)
                                      .addModifiers(PUBLIC)
                                      .addStatement("delegate.close()")
                                      .build();

        type.addMethod(method);
    }

    @Override
    protected List<MethodSpec> operations() {
        return model.getOperations().values().stream()
                    // TODO Sync not supported for event streaming yet. Revisit after sync/async merge
                    .filter(o -> !o.hasEventStreamInput())
                    .filter(o -> !o.hasEventStreamOutput())
                    .flatMap(this::operations)
                    .sorted(Comparator.comparing(m -> m.name))
                    .collect(toList());
    }

    private Stream<MethodSpec> operations(OperationModel opModel) {
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(traditionalMethod(opModel));
        return methods.stream();
    }

    @Override
    public ClassName className() {
        return className;
    }

    @Override
    protected MethodSpec.Builder simpleMethodModifier(MethodSpec.Builder builder) {
        return builder.addAnnotation(Override.class);
    }

    protected MethodSpec traditionalMethod(OperationModel opModel) {
        MethodSpec.Builder builder = operationMethodSignature(model, opModel);
        return operationBody(builder, opModel).build();
    }

    @Override
    protected MethodSpec.Builder operationBody(MethodSpec.Builder builder, OperationModel opModel) {
        builder.addModifiers(PUBLIC)
               .addAnnotation(Override.class);

        if (builder.parameters.isEmpty()) {
            throw new IllegalStateException("All client methods must have an argument");
        }

        List<ParameterSpec> operationParameters = new ArrayList<>(builder.parameters);
        String requestParameter = operationParameters.remove(0).name;
        String additionalParameters = String.format(", %s", operationParameters.stream().map(p -> p.name).collect(joining(", ")));

        builder.addStatement("return invokeOperation($N, request -> delegate.$N(request$N))",
                             requestParameter,
                             opModel.getMethodName(),
                             operationParameters.isEmpty() ? "" : additionalParameters);

        return builder;
    }

    @Override
    protected MethodSpec.Builder utilitiesOperationBody(MethodSpec.Builder builder) {
        return builder.addAnnotation(Override.class).addStatement("return delegate.$N()", UtilitiesMethod.METHOD_NAME);
    }

    @Override
    protected MethodSpec.Builder waiterOperationBody(MethodSpec.Builder builder) {
        return builder.addAnnotation(Override.class).addStatement("return delegate.waiter()");
    }

    private MethodSpec constructor(ClassName interfaceClass) {
        return MethodSpec.constructorBuilder()
                         .addModifiers(PUBLIC)
                         .addParameter(interfaceClass, "delegate")
                         .addStatement("$T.paramNotNull(delegate, \"delegate\")", Validate.class)
                         .addStatement("this.delegate = delegate")
                         .build();
    }

    private MethodSpec nameMethod() {
        return MethodSpec.methodBuilder("serviceName")
                         .addAnnotation(Override.class)
                         .addModifiers(PUBLIC, FINAL)
                         .returns(String.class)
                         .addStatement("return delegate.serviceName()")
                         .build();
    }

    private MethodSpec delegateMethod() {
        return MethodSpec.methodBuilder("delegate")
                         .addModifiers(PUBLIC)
                         .addStatement("return this.delegate")
                         .returns(SdkClient.class)
                         .build();
    }

    private MethodSpec invokeMethod() {
        TypeVariableName requestTypeVariableName =
            TypeVariableName.get("T", poetExtensions.getModelClass(model.getSdkRequestBaseClassName()));

        TypeVariableName responseTypeVariableName = STREAMING_TYPE_VARIABLE;

        ParameterizedTypeName functionTypeName = ParameterizedTypeName
            .get(ClassName.get(Function.class), requestTypeVariableName, responseTypeVariableName);

        return MethodSpec.methodBuilder("invokeOperation")
                         .addModifiers(PROTECTED)
                         .addParameter(requestTypeVariableName, "request")
                         .addParameter(functionTypeName, "operation")
                         .addTypeVariable(requestTypeVariableName)
                         .addTypeVariable(responseTypeVariableName)
                         .returns(responseTypeVariableName)
                         .addStatement("return operation.apply(request)")
                         .build();
    }

    @Override
    protected MethodSpec serviceClientConfigMethod() {
        return MethodSpec.methodBuilder("serviceClientConfiguration")
                         .addAnnotation(Override.class)
                         .addModifiers(PUBLIC, FINAL)
                         .returns(new PoetExtension(model).getServiceConfigClass())
                         .addStatement("return delegate.serviceClientConfiguration()")
                         .build();
    }
}
