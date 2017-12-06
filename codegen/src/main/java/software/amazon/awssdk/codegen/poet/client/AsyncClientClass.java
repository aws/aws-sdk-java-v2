/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static com.squareup.javapoet.TypeSpec.Builder;
import static software.amazon.awssdk.codegen.poet.client.ClientClassUtils.getCustomResponseHandler;
import static software.amazon.awssdk.codegen.poet.client.SyncClientClass.getProtocolSpecs;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.client.specs.ProtocolSpec;
import software.amazon.awssdk.core.client.AsyncClientHandler;
import software.amazon.awssdk.core.client.SdkAsyncClientHandler;
import software.amazon.awssdk.core.config.AsyncClientConfiguration;

public final class AsyncClientClass extends AsyncClientInterface {
    private final PoetExtensions poetExtensions;
    private final ClassName className;
    private final ProtocolSpec protocolSpec;
    private final String basePackage;

    public AsyncClientClass(GeneratorTaskParams dependencies) {
        super(dependencies.getModel());
        this.poetExtensions = dependencies.getPoetExtensions();
        this.className = poetExtensions.getClientClass(model.getMetadata().getAsyncClient());
        this.protocolSpec = getProtocolSpecs(poetExtensions, model.getMetadata().getProtocol());
        this.basePackage = dependencies.getModel().getMetadata().getFullClientPackageName();
    }

    @Override
    public TypeSpec poetSpec() {
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getAsyncInterface());
        Builder classBuilder = PoetUtils.createClassBuilder(className)
                                        .addAnnotation(SdkInternalApi.class)
                                        .addModifiers(Modifier.FINAL)
                                        .addField(AsyncClientHandler.class, "clientHandler", Modifier.PRIVATE, Modifier.FINAL)
                                        .addField(protocolSpec.protocolFactory(model))
                                        .addSuperinterface(interfaceClass)
                                        .addJavadoc("Internal implementation of {@link $1T}.\n\n@see $1T#builder()",
                                                    interfaceClass)
                                        .addMethod(nameMethod())
                                        .addMethods(operations())
                                        .addMethods(paginatedTraditionalMethods())
                                        .addMethod(closeMethod())
                                        .addMethods(protocolSpec.additionalMethods())
                                        .addMethod(protocolSpec.initProtocolFactory(model));

        if (model.getCustomizationConfig().getServiceSpecificClientConfigClass() != null) {
            classBuilder.addMethod(constructorWithAdvancedConfiguration());
        } else {
            classBuilder.addMethod(constructor());
        }

        protocolSpec.createErrorResponseHandler().ifPresent(classBuilder::addMethod);

        return classBuilder.build();
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PROTECTED)
                         .addParameter(AsyncClientConfiguration.class, "clientConfiguration")
                         .addStatement("this.clientHandler = new $T(clientConfiguration, null)",
                                       SdkAsyncClientHandler.class) // TODO this will likely differ for APIG clients
                         .addStatement("this.$N = init()", protocolSpec.protocolFactory(model).name)
                         .build();
    }

    private MethodSpec nameMethod() {
        return MethodSpec.methodBuilder("serviceName")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                         .returns(String.class)
                         .addStatement("return SERVICE_NAME")
                         .build();
    }

    private MethodSpec constructorWithAdvancedConfiguration() {
        ClassName advancedConfiguration = ClassName.get(basePackage,
                                                        model.getCustomizationConfig().getServiceSpecificClientConfigClass());
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PROTECTED)
                         .addParameter(AsyncClientConfiguration.class, "clientConfiguration")
                         .addParameter(advancedConfiguration, "serviceConfiguration")
                         .addStatement("this.clientHandler = new $T(clientConfiguration, serviceConfiguration)",
                                       SdkAsyncClientHandler.class) // TODO this will likely differ for APIG clients
                         .addStatement("this.$N = init()", protocolSpec.protocolFactory(model).name)
                         .build();
    }

    private MethodSpec closeMethod() {
        return MethodSpec.methodBuilder("close")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addStatement("$N.close()", "clientHandler")
                         .build();
    }

    @Override
    protected MethodSpec.Builder operationBody(MethodSpec.Builder builder, OperationModel opModel) {        
        ClassName returnType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());

        return builder.addModifiers(Modifier.PUBLIC)
                      .addAnnotation(Override.class)
                      .addCode(getCustomResponseHandler(opModel, returnType)
                                   .orElseGet(() -> protocolSpec.responseHandler(opModel)))
                      .addCode(protocolSpec.errorResponseHandler(opModel))
                      .addCode(protocolSpec.asyncExecutionHandler(opModel));

    }

    @Override
    protected MethodSpec.Builder paginatedMethodBody(MethodSpec.Builder builder, OperationModel opModel) {
        return builder.addModifiers(Modifier.PUBLIC)
                      .addStatement("return new $T(this, $L)",
                                    poetExtensions.getResponseClassForPaginatedAsyncOperation(opModel.getOperationName()),
                                    opModel.getInput().getVariableName());
    }

    @Override
    public ClassName className() {
        return className;
    }
}
