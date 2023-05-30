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
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.docs.SimpleMethodOverload;
import software.amazon.awssdk.codegen.model.config.customization.UtilitiesMethod;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.utils.PaginatorUtils;
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
        MethodSpec delegate = MethodSpec.methodBuilder("delegate")
                                        .addModifiers(PUBLIC)
                                        .addStatement("return this.delegate")
                                        .returns(SdkClient.class)
                                        .build();

        type.addMethod(nameMethod())
            .addMethod(delegate);
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
    public ClassName className() {
        return className;
    }

    @Override
    protected MethodSpec.Builder simpleMethodModifier(MethodSpec.Builder builder) {
        return builder.addAnnotation(Override.class);
    }

    @Override
    protected MethodSpec.Builder operationBody(MethodSpec.Builder builder, OperationModel opModel) {
        builder.addModifiers(PUBLIC)
               .addAnnotation(Override.class);

        builder.addStatement("return delegate.$N($L)",
                             opModel.getMethodName(),
                             builder.parameters.stream().map(p -> p.name).collect(joining(", ")));
        return builder;
    }

    @Override
    protected MethodSpec.Builder paginatedMethodBody(MethodSpec.Builder builder, OperationModel opModel) {
        String methodName = PaginatorUtils.getPaginatedMethodName(opModel.getMethodName());
        return builder.addModifiers(PUBLIC)
                      .addAnnotation(Override.class)
                      .addStatement("return delegate.$N($N)", methodName, opModel.getInput().getVariableName());
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
}
