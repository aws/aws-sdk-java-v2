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

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static software.amazon.awssdk.codegen.poet.client.ClientClassUtils.getCustomResponseHandler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.config.AwsSyncClientConfiguration;
import software.amazon.awssdk.codegen.docs.SimpleMethodOverload;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.client.specs.Ec2ProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.JsonProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.ProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.QueryXmlProtocolSpec;
import software.amazon.awssdk.codegen.utils.PaginatorUtils;
import software.amazon.awssdk.core.client.SyncClientHandler;

public class SyncClientClass implements ClassSpec {

    private final IntermediateModel model;
    private final String basePackage;
    private final PoetExtensions poetExtensions;
    private final ClassName className;
    private final ProtocolSpec protocolSpec;

    public SyncClientClass(GeneratorTaskParams taskParams) {
        this.model = taskParams.getModel();
        this.basePackage = model.getMetadata().getFullClientPackageName();
        this.poetExtensions = taskParams.getPoetExtensions();
        this.className = poetExtensions.getClientClass(model.getMetadata().getSyncClient());
        this.protocolSpec = getProtocolSpecs(poetExtensions, model.getMetadata().getProtocol());
    }

    @Override
    public TypeSpec poetSpec() {
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());

        Builder classBuilder = PoetUtils.createClassBuilder(className)
                                        .addAnnotation(SdkInternalApi.class)
                                        .addModifiers(FINAL)
                                        .addSuperinterface(interfaceClass)
                                        .addJavadoc("Internal implementation of {@link $1T}.\n\n@see $1T#builder()",
                                                    interfaceClass)
                                        .addField(SyncClientHandler.class, "clientHandler", PRIVATE, FINAL)
                                        .addField(protocolSpec.protocolFactory(model))
                                        .addField(AwsSyncClientConfiguration.class, "clientConfiguration", PRIVATE, FINAL)
                                        .addMethod(nameMethod())
                                        .addMethods(operations());

        if (model.getCustomizationConfig().getServiceSpecificClientConfigClass() != null) {
            classBuilder.addMethod(constructorWithAdvancedConfiguration());
        } else {
            classBuilder.addMethod(constructor());
        }

        protocolSpec.createErrorResponseHandler().ifPresent(classBuilder::addMethod);

        classBuilder.addMethod(protocolSpec.initProtocolFactory(model));

        classBuilder.addMethod(closeMethod());

        classBuilder.addMethods(protocolSpec.additionalMethods());

        return classBuilder.build();
    }

    private MethodSpec nameMethod() {
        return MethodSpec.methodBuilder("serviceName")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                         .returns(String.class)
                         .addStatement("return SERVICE_NAME")
                         .build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PROTECTED)
                         .addParameter(AwsSyncClientConfiguration.class, "clientConfiguration")
                         .addStatement("this.clientHandler = new $T(clientConfiguration, null)",
                                       protocolSpec.getClientHandlerClass())
                         .addStatement("this.$N = init()", protocolSpec.protocolFactory(model).name)
                         .addStatement("this.clientConfiguration = clientConfiguration")
                         .build();
    }

    private MethodSpec constructorWithAdvancedConfiguration() {
        ClassName advancedConfiguration = ClassName.get(basePackage,
                                                        model.getCustomizationConfig().getServiceSpecificClientConfigClass());
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PROTECTED)
                         .addParameter(AwsSyncClientConfiguration.class, "clientConfiguration")
                         .addParameter(advancedConfiguration, "serviceConfiguration")
                         .addStatement("this.clientHandler = new $T(clientConfiguration, serviceConfiguration)",
                                       protocolSpec.getClientHandlerClass())
                         .addStatement("this.$N = init()", protocolSpec.protocolFactory(model).name)
                         .addStatement("this.clientConfiguration = clientConfiguration")
                         .build();
    }

    private List<MethodSpec> operations() {
        return model.getOperations().values().stream()
                    .map(this::operationMethodSpecs)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
    }

    private List<MethodSpec> operationMethodSpecs(OperationModel opModel) {
        List<MethodSpec> methods = new ArrayList<>();
        ClassName returnType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());

        methods.add(SyncClientInterface.operationMethodSignature(model, opModel)
                                  .addAnnotation(Override.class)
                                  .addCode(getCustomResponseHandler(opModel, returnType)
                                               .orElseGet(() -> protocolSpec.responseHandler(opModel)))
                                  .addCode(protocolSpec.errorResponseHandler(opModel))
                                  .addCode(protocolSpec.executionHandler(opModel))
                                  .build());

        methods.addAll(paginatedMethods(opModel));

        return methods;
    }

    private List<MethodSpec> paginatedMethods(OperationModel opModel) {
        List<MethodSpec> paginatedMethodSpecs = new ArrayList<>();

        if (opModel.isPaginated()) {
            paginatedMethodSpecs.add(SyncClientInterface.operationMethodSignature(model,
                                                                                  opModel,
                                                                                  SimpleMethodOverload.PAGINATED,
                                                                                  PaginatorUtils.getPaginatedMethodName(
                                                                                      opModel.getMethodName()))
                                                        .addAnnotation(Override.class)
                                                        .returns(poetExtensions.getResponseClassForPaginatedSyncOperation(
                                                            opModel.getOperationName()))
                                                        .addStatement("return new $T(this, $L)",
                                                                      poetExtensions.getResponseClassForPaginatedSyncOperation(
                                                                          opModel.getOperationName()),
                                                                      opModel.getInput().getVariableName())
                                                        .build());
        }

        return paginatedMethodSpecs;
    }

    private MethodSpec closeMethod() {
        return MethodSpec.methodBuilder("close")
                         .addAnnotation(Override.class)
                         .addStatement("clientHandler.close()")
                         .addModifiers(Modifier.PUBLIC)
                         .build();
    }

    static ProtocolSpec getProtocolSpecs(PoetExtensions poetExtensions, Protocol protocol) {
        switch (protocol) {
            case QUERY:
            case REST_XML:
                return new QueryXmlProtocolSpec(poetExtensions);
            case EC2:
                return new Ec2ProtocolSpec(poetExtensions);
            case AWS_JSON:
            case REST_JSON:
            case CBOR:
            case ION:
                return new JsonProtocolSpec(poetExtensions);
            case API_GATEWAY:
                throw new UnsupportedOperationException("Not yet supported.");
            default:
                throw new RuntimeException("Unknown protocol: " + protocol.name());
        }
    }
}
