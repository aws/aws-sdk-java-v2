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
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.model.config.customization.UtilitiesMethod;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.utils.Validate;

public class DelegatingAsyncClientClass extends AsyncClientInterface {

    private final IntermediateModel model;
    private final ClassName className;
    private final PoetExtension poetExtensions;

    public DelegatingAsyncClientClass(IntermediateModel model) {
        super(model);
        this.model = model;
        this.className = ClassName.get(model.getMetadata().getFullClientPackageName(),
                                       "Delegating" + model.getMetadata().getAsyncInterface());
        this.poetExtensions = new PoetExtension(model);
    }

    @Override
    protected TypeSpec.Builder createTypeSpec() {
        return PoetUtils.createClassBuilder(className);
    }

    @Override
    protected void addInterfaceClass(TypeSpec.Builder type) {
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getAsyncInterface());

        type.addSuperinterface(interfaceClass)
            .addMethod(constructor(interfaceClass));
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
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getAsyncInterface());

        type.addField(FieldSpec.builder(interfaceClass, "delegate")
                           .addModifiers(PRIVATE, FINAL)
                           .build());
    }

    @Override
    protected void addAdditionalMethods(TypeSpec.Builder type) {
        type.addMethod(nameMethod())
            .addMethod(delegateMethod())
            .addMethod(invokeMethod());
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

        ParameterizedTypeName responseFutureTypeName = ParameterizedTypeName.get(ClassName.get(CompletableFuture.class),
                                                                                 responseTypeVariableName);

        ParameterizedTypeName functionTypeName = ParameterizedTypeName
            .get(ClassName.get(Function.class), requestTypeVariableName, responseFutureTypeName);

        return MethodSpec.methodBuilder("invokeOperation")
                         .addModifiers(PROTECTED)
                         .addParameter(requestTypeVariableName, "request")
                         .addParameter(functionTypeName, "operation")
                         .addTypeVariable(requestTypeVariableName)
                         .addTypeVariable(responseTypeVariableName)
                         .returns(responseFutureTypeName)
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
                    .flatMap(this::operations)
                    .sorted(Comparator.comparing(m -> m.name))
                    .collect(toList());
    }

    private Stream<MethodSpec> operations(OperationModel opModel) {
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(traditionalMethod(opModel));
        return methods.stream();
    }

    private MethodSpec constructor(ClassName interfaceClass) {
        return MethodSpec.constructorBuilder()
                         .addModifiers(PUBLIC)
                         .addParameter(interfaceClass, "delegate")
                         .addStatement("$T.paramNotNull(delegate, \"delegate\")", Validate.class)
                         .addStatement("this.delegate = delegate")
                         .build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    @Override
    protected MethodSpec.Builder operationBody(MethodSpec.Builder builder, OperationModel opModel) {
        builder.addModifiers(PUBLIC)
               .addAnnotation(Override.class);

        if (builder.parameters.isEmpty()) {
            throw new IllegalStateException("All client methods must have an argument");
        }

        List<ParameterSpec> parameters = new ArrayList<>(builder.parameters);
        String requestParameter = parameters.remove(0).name;
        String additionalParameters = String.format(", %s", parameters.stream().map(p -> p.name).collect(joining(", ")));

        builder.addStatement("return invokeOperation($N, request -> delegate.$N(request$N))",
                             requestParameter,
                             opModel.getMethodName(),
                             parameters.isEmpty() ? "" : additionalParameters);

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

    @Override
    protected MethodSpec.Builder batchManagerOperationBody(MethodSpec.Builder builder) {
        return builder.addAnnotation(Override.class).addStatement("return delegate.batchManager()");
    }
}
