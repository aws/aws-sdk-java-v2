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

package software.amazon.awssdk.codegen.poet.endpointdiscovery;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryCacheLoader;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryEndpoint;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryRequest;

public class EndpointDiscoveryCacheLoaderGenerator implements ClassSpec {

    private static final String CLIENT_FIELD = "client";

    private final IntermediateModel model;
    private final PoetExtensions poetExtensions;

    public EndpointDiscoveryCacheLoaderGenerator(GeneratorTaskParams generatorTaskParams) {
        this.model = generatorTaskParams.getModel();
        this.poetExtensions = generatorTaskParams.getPoetExtensions();
    }


    @Override
    public TypeSpec poetSpec() {
        return TypeSpec.classBuilder(className())
                       .addAnnotation(SdkInternalApi.class)
                       .addAnnotation(PoetUtils.generatedAnnotation())
                       .addSuperinterface(EndpointDiscoveryCacheLoader.class)
                       .addField(FieldSpec.builder(poetExtensions.getClientClass(model.getMetadata().getSyncInterface()),
                                                   CLIENT_FIELD)
                                          .addModifiers(FINAL, PRIVATE)
                                          .build())
                       .addMethod(constructor())
                       .addMethod(create())
                       .addMethod(discoverEndpoint(model.getEndpointOperation().get()))
                       .build();
    }

    @Override
    public ClassName className() {
        return poetExtensions.getClientClass(model.getNamingStrategy().getServiceName() +
                                             "EndpointDiscoveryCacheLoader");
    }

    private MethodSpec create() {
        return MethodSpec.methodBuilder("create")
                         .addModifiers(STATIC, PUBLIC)
                         .returns(className())
                         .addParameter(poetExtensions.getClientClass(model.getMetadata().getSyncInterface()), CLIENT_FIELD)
                         .addStatement("return new $T($L)", className(), CLIENT_FIELD)
                         .build();
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(PRIVATE)
                         .addParameter(poetExtensions.getClientClass(model.getMetadata().getSyncInterface()), CLIENT_FIELD)
                         .addStatement("this.$L = $L", CLIENT_FIELD, CLIENT_FIELD)
                         .build();
    }

    private MethodSpec discoverEndpoint(OperationModel opModel) {
        ParameterizedTypeName returnType = ParameterizedTypeName.get(CompletableFuture.class, EndpointDiscoveryEndpoint.class);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("discoverEndpoint")
                                                     .addModifiers(PUBLIC)
                                                     .addAnnotation(Override.class)
                                                     .addParameter(EndpointDiscoveryRequest.class, "endpointDiscoveryRequest")
                                                     .returns(returnType);

        if (!opModel.getInputShape().isHasHeaderMember()) {
            methodBuilder.addCode("return $T.supplyAsync(() -> {", CompletableFuture.class)
                         .addStatement("$T response = $L.$L($L.builder().build())",
                                       poetExtensions.getModelClass(opModel.getOutputShape().getC2jName()),
                                       CLIENT_FIELD,
                                       opModel.getMethodName(),
                                       poetExtensions.getModelClass(opModel.getInputShape().getC2jName()))
                         .addStatement("$T endpoint = response.endpoints().get(0)",
                                       poetExtensions.getModelClass("Endpoint"))
                         .addStatement("return $T.builder().endpoint(toUri(endpoint.address(), $L.defaultEndpoint()))" +
                                       ".expirationTime($T.now().plus(endpoint.cachePeriodInMinutes(), $T.MINUTES)).build()",
                                       EndpointDiscoveryEndpoint.class,
                                       "endpointDiscoveryRequest",
                                       Instant.class,
                                       ChronoUnit.class)
                         .addStatement("})");

        }

        return methodBuilder.build();
    }
}
