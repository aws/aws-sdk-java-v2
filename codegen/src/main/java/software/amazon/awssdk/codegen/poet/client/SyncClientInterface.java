/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static software.amazon.awssdk.codegen.poet.client.AsyncClientInterface.STREAMING_TYPE_VARIABLE;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.codegen.docs.ClientType;
import software.amazon.awssdk.codegen.docs.DocConfiguration;
import software.amazon.awssdk.codegen.docs.SimpleMethodOverload;
import software.amazon.awssdk.codegen.model.config.customization.UtilitiesMethod;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.utils.PaginatorUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

public final class SyncClientInterface implements ClassSpec {

    private final IntermediateModel model;
    private final ClassName className;
    private final String clientPackageName;
    private final PoetExtensions poetExtensions;

    public SyncClientInterface(IntermediateModel model) {
        this.model = model;
        this.clientPackageName = model.getMetadata().getFullClientPackageName();
        this.className = ClassName.get(clientPackageName, model.getMetadata().getSyncInterface());
        this.poetExtensions = new PoetExtensions(model);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder result = PoetUtils.createInterfaceBuilder(className);

        result.addSuperinterface(SdkClient.class)
              .addField(FieldSpec.builder(String.class, "SERVICE_NAME")
                                 .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                 .initializer("$S", model.getMetadata().getSigningName())
                                 .build());

        PoetUtils.addJavadoc(result::addJavadoc, getJavadoc());

        if (!model.getCustomizationConfig().isExcludeClientCreateMethod()) {
            result.addMethod(create());
        }

        result.addMethod(builder())
              .addMethods(operations())
              .addMethod(serviceMetadata());

        if (model.getCustomizationConfig().getUtilitiesMethod() != null) {
            result.addMethod(utilitiesMethod());
        }

        return result.build();
    }


    @Override
    public ClassName className() {
        return className;
    }

    private String getJavadoc() {
        return "Service client for accessing " + model.getMetadata().getDescriptiveServiceName() + ". This can be "
               + "created using the static {@link #builder()} method.\n\n" + model.getMetadata().getDocumentation();
    }

    private MethodSpec create() {
        return MethodSpec.methodBuilder("create")
                         .returns(className)
                         .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                         .addJavadoc(
                                 "Create a {@link $T} with the region loaded from the {@link $T} and credentials loaded from the "
                                 + "{@link $T}.", className, DefaultAwsRegionProviderChain.class,
                                 DefaultCredentialsProvider.class)
                         .addStatement("return builder().build()")
                         .build();
    }

    private MethodSpec builder() {
        ClassName builderClass = ClassName.get(clientPackageName, model.getMetadata().getSyncBuilder());
        ClassName builderInterface = ClassName.get(clientPackageName, model.getMetadata().getSyncBuilderInterface());
        return MethodSpec.methodBuilder("builder")
                         .returns(builderInterface)
                         .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                         .addJavadoc("Create a builder that can be used to configure and create a {@link $T}.", className)
                         .addStatement("return new $T()", builderClass)
                         .build();
    }

    private Iterable<MethodSpec> operations() {
        return model.getOperations().values().stream()
                    // TODO Sync not supported for event streaming yet. Revisit after sync/async merge
                    .filter(o -> !o.hasEventStreamInput())
                    .filter(o -> !o.hasEventStreamOutput())
                    .map(this::operationMethodSpec)
                    .flatMap(List::stream)
                    .collect(toList());
    }

    private MethodSpec serviceMetadata() {
        return MethodSpec.methodBuilder("serviceMetadata")
                         .returns(ServiceMetadata.class)
                         .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                         .addStatement("return $T.of($S)", ServiceMetadata.class, model.getMetadata().getEndpointPrefix())
                         .build();
    }

    private List<MethodSpec> operationMethodSpec(OperationModel opModel) {
        List<MethodSpec> methods = new ArrayList<>();

        if (opModel.getInputShape().isSimpleMethod()) {
            methods.add(simpleMethod(opModel));
        }

        methods.addAll(operation(opModel));

        methods.addAll(streamingSimpleMethods(opModel));
        methods.addAll(paginatedMethods(opModel));

        return methods;
    }

    private MethodSpec simpleMethod(OperationModel opModel) {
        ClassName requestType = ClassName.get(model.getMetadata().getFullModelPackageName(),
                                              opModel.getInput().getVariableType());
        return operationSimpleMethodSignature(model, opModel, opModel.getMethodName())
            .addStatement("return $L($T.builder().build())", opModel.getMethodName(), requestType)
            .addJavadoc(opModel.getDocs(model, ClientType.SYNC, SimpleMethodOverload.NO_ARG))
            .build();
    }

    private static MethodSpec.Builder operationBaseSignature(IntermediateModel model,
                                                             OperationModel opModel,
                                                             Consumer<MethodSpec.Builder> addFirstParameter,
                                                             SimpleMethodOverload simpleMethodOverload,
                                                             String methodName) {

        TypeName responseType = ClassName.get(model.getMetadata().getFullModelPackageName(),
                                              opModel.getReturnType().getReturnType());
        TypeName returnType = opModel.hasStreamingOutput() ? STREAMING_TYPE_VARIABLE : responseType;

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                                                           .returns(returnType)
                                                           .addModifiers(Modifier.PUBLIC)
                                                           .addJavadoc(opModel.getDocs(model, ClientType.SYNC,
                                                                                       simpleMethodOverload))
                                                           .addExceptions(getExceptionClasses(model, opModel));

        addFirstParameter.accept(methodBuilder);
        streamingMethod(methodBuilder, opModel, responseType);

        return methodBuilder;
    }

    private List<MethodSpec> operation(OperationModel opModel) {
        List<MethodSpec> methods = new ArrayList<>();

        methods.add(operationMethodSignature(model, opModel)
                            .addModifiers(Modifier.DEFAULT)
                            .addStatement("throw new $T()", UnsupportedOperationException.class)
                            .build());

        methods.add(ClientClassUtils.consumerBuilderVariant(methods.get(0),
                                                            consumerBuilderJavadoc(opModel,
                                                                                   SimpleMethodOverload.NORMAL)));

        return methods;
    }

    static MethodSpec.Builder operationMethodSignature(IntermediateModel model,
                                                       OperationModel opModel) {
        return operationMethodSignature(model, opModel, SimpleMethodOverload.NORMAL, opModel.getMethodName());
    }

    // TODO This is inconsistent with how async client reuses method signature
    static MethodSpec.Builder operationMethodSignature(IntermediateModel model,
                                                       OperationModel opModel,
                                                       SimpleMethodOverload simpleMethodOverload,
                                                       String methodName) {
        ClassName requestType = ClassName.get(model.getMetadata().getFullModelPackageName(),
                                              opModel.getInput().getVariableType());

        return operationBaseSignature(model, opModel, b -> b.addParameter(requestType, opModel.getInput().getVariableName()),
                                      simpleMethodOverload, methodName);
    }

    private MethodSpec.Builder operationSimpleMethodSignature(IntermediateModel model,
                                                              OperationModel opModel,
                                                              String methodName) {
        TypeName returnType = ClassName.get(model.getMetadata().getFullModelPackageName(),
                                            opModel.getReturnType().getReturnType());

        return MethodSpec.methodBuilder(methodName)
                         .returns(returnType)
                         .addModifiers(Modifier.PUBLIC)
                         .addModifiers(Modifier.DEFAULT)
                         .addExceptions(getExceptionClasses(model, opModel));
    }

    private List<MethodSpec> paginatedMethods(OperationModel opModel) {
        List<MethodSpec> paginatedMethodSpecs = new ArrayList<>();

        if (opModel.isPaginated()) {
            if (opModel.getInputShape().isSimpleMethod()) {
                paginatedMethodSpecs.add(paginatedSimpleMethod(opModel));
            }

            MethodSpec paginatedMethod =
                    operationMethodSignature(model,
                                             opModel,
                                             SimpleMethodOverload.PAGINATED,
                                             PaginatorUtils.getPaginatedMethodName(opModel.getMethodName()))
                            .returns(poetExtensions.getResponseClassForPaginatedSyncOperation(opModel.getOperationName()))
                            .addModifiers(Modifier.DEFAULT)
                            .addStatement("throw new $T()", UnsupportedOperationException.class)
                            .build();

            paginatedMethodSpecs.add(paginatedMethod);

            String consumerBuilderJavadoc = consumerBuilderJavadoc(opModel, SimpleMethodOverload.PAGINATED);
            paginatedMethodSpecs.add(ClientClassUtils.consumerBuilderVariant(paginatedMethod, consumerBuilderJavadoc));
        }

        return paginatedMethodSpecs;
    }

    private MethodSpec paginatedSimpleMethod(OperationModel opModel) {
        String paginatedMethodName = PaginatorUtils.getPaginatedMethodName(opModel.getMethodName());
        ClassName requestType = ClassName.get(model.getMetadata().getFullModelPackageName(),
                                              opModel.getInput().getVariableType());

        return operationSimpleMethodSignature(model, opModel, paginatedMethodName)
                .returns(poetExtensions.getResponseClassForPaginatedSyncOperation(opModel.getOperationName()))
                .addStatement("return $L($T.builder().build())", paginatedMethodName, requestType)
                .addJavadoc(opModel.getDocs(model, ClientType.SYNC, SimpleMethodOverload.NO_ARG_PAGINATED))
                .build();
    }

    private static void streamingMethod(MethodSpec.Builder methodBuilder, OperationModel opModel, TypeName responseType) {
        if (opModel.hasStreamingInput()) {
            methodBuilder.addParameter(ClassName.get(RequestBody.class), "requestBody");
        }
        if (opModel.hasStreamingOutput()) {
            methodBuilder.addTypeVariable(STREAMING_TYPE_VARIABLE);
            ParameterizedTypeName streamingResponseHandlerType = ParameterizedTypeName
                    .get(ClassName.get(ResponseTransformer.class), responseType, STREAMING_TYPE_VARIABLE);
            methodBuilder.addParameter(streamingResponseHandlerType, "responseTransformer");
        }
    }

    private List<MethodSpec> streamingSimpleMethods(OperationModel opModel) {
        String fileConsumerBuilderJavadoc = consumerBuilderJavadoc(opModel, SimpleMethodOverload.FILE);

        TypeName responseType = ClassName.get(model.getMetadata().getFullModelPackageName(),
                                              opModel.getReturnType().getReturnType());
        ClassName requestType = ClassName.get(model.getMetadata().getFullModelPackageName(),
                                              opModel.getInput().getVariableType());

        List<MethodSpec> simpleMethods = new ArrayList<>();

        if (opModel.hasStreamingInput() && opModel.hasStreamingOutput()) {
            MethodSpec simpleMethod = streamingInputOutputFileSimpleMethod(opModel, responseType, requestType);
            simpleMethods.add(simpleMethod);
            simpleMethods.add(ClientClassUtils.consumerBuilderVariant(simpleMethod, fileConsumerBuilderJavadoc));

        } else if (opModel.hasStreamingInput()) {
            MethodSpec simpleMethod = uploadFromFileSimpleMethod(opModel, responseType, requestType);
            simpleMethods.add(simpleMethod);
            simpleMethods.add(ClientClassUtils.consumerBuilderVariant(simpleMethod, fileConsumerBuilderJavadoc));

        } else if (opModel.hasStreamingOutput()) {
            String inputStreamConsumerBuilderJavadoc = consumerBuilderJavadoc(opModel, SimpleMethodOverload.INPUT_STREAM);
            String bytesConsumerBuilderJavadoc = consumerBuilderJavadoc(opModel, SimpleMethodOverload.BYTES);

            MethodSpec downloadToFileSimpleMethod = downloadToFileSimpleMethod(opModel, responseType, requestType);
            MethodSpec inputStreamSimpleMethod = inputStreamSimpleMethod(opModel, responseType, requestType);
            MethodSpec bytesSimpleMethod = bytesSimpleMethod(opModel, responseType, requestType);

            MethodSpec downloadToFileConsumerBuilder = ClientClassUtils.consumerBuilderVariant(downloadToFileSimpleMethod,
                                                                                               fileConsumerBuilderJavadoc);
            MethodSpec inputStreamConsumerBuilder = ClientClassUtils.consumerBuilderVariant(inputStreamSimpleMethod,
                                                                                            inputStreamConsumerBuilderJavadoc);
            MethodSpec bytesConsumerBuilder = ClientClassUtils.consumerBuilderVariant(bytesSimpleMethod,
                                                                                      bytesConsumerBuilderJavadoc);

            simpleMethods.add(downloadToFileSimpleMethod);
            simpleMethods.add(downloadToFileConsumerBuilder);
            simpleMethods.add(inputStreamSimpleMethod);
            simpleMethods.add(inputStreamConsumerBuilder);
            simpleMethods.add(bytesSimpleMethod);
            simpleMethods.add(bytesConsumerBuilder);
        }

        return simpleMethods;
    }

    /**
     * @return Simple method for streaming input operations to read data from a file.
     */
    private MethodSpec uploadFromFileSimpleMethod(OperationModel opModel, TypeName responseType, ClassName requestType) {
        return MethodSpec.methodBuilder(opModel.getMethodName())
                         .returns(responseType)
                         .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                         .addParameter(requestType, opModel.getInput().getVariableName())
                         .addParameter(ClassName.get(Path.class), "filePath")
                         .addJavadoc(opModel.getDocs(model, ClientType.SYNC, SimpleMethodOverload.FILE))
                         .addExceptions(getExceptionClasses(model, opModel))
                         .addStatement("return $L($L, $T.fromFile($L))", opModel.getMethodName(),
                                       opModel.getInput().getVariableName(),
                                       ClassName.get(RequestBody.class),
                                       "filePath")
                         .build();
    }

    /**
     * @return Simple method for streaming output operations to get content as an input stream.
     */
    private MethodSpec inputStreamSimpleMethod(OperationModel opModel, TypeName responseType, ClassName requestType) {
        TypeName returnType = ParameterizedTypeName.get(ClassName.get(ResponseInputStream.class), responseType);
        return MethodSpec.methodBuilder(opModel.getMethodName())
                         .returns(returnType)
                         .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                         .addParameter(requestType, opModel.getInput().getVariableName())
                         .addJavadoc(opModel.getDocs(model, ClientType.SYNC, SimpleMethodOverload.INPUT_STREAM))
                         .addExceptions(getExceptionClasses(model, opModel))
                         .addStatement("return $L($L, $T.toInputStream())", opModel.getMethodName(),
                                       opModel.getInput().getVariableName(),
                                       ClassName.get(ResponseTransformer.class))
                         .build();
    }

    /**
     * @return Simple method for streaming output operations to get the content as a byte buffer or other in-memory types.
     */
    private MethodSpec bytesSimpleMethod(OperationModel opModel, TypeName responseType, ClassName requestType) {
        TypeName returnType = ParameterizedTypeName.get(ClassName.get(ResponseBytes.class), responseType);
        return MethodSpec.methodBuilder(opModel.getMethodName() + "AsBytes")
                         .returns(returnType)
                         .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                         .addParameter(requestType, opModel.getInput().getVariableName())
                         .addJavadoc(opModel.getDocs(model, ClientType.SYNC, SimpleMethodOverload.BYTES))
                         .addExceptions(getExceptionClasses(model, opModel))
                         .addStatement("return $L($L, $T.toBytes())", opModel.getMethodName(),
                                       opModel.getInput().getVariableName(),
                                       ClassName.get(ResponseTransformer.class))
                         .build();
    }

    /**
     * @return Simple method for streaming output operations to write response content to a file.
     */
    private MethodSpec downloadToFileSimpleMethod(OperationModel opModel, TypeName responseType, ClassName requestType) {
        return MethodSpec.methodBuilder(opModel.getMethodName())
                         .returns(responseType)
                         .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                         .addParameter(requestType, opModel.getInput().getVariableName())
                         .addParameter(ClassName.get(Path.class), "filePath")
                         .addJavadoc(opModel.getDocs(model, ClientType.SYNC, SimpleMethodOverload.FILE))
                         .addExceptions(getExceptionClasses(model, opModel))
                         .addStatement("return $L($L, $T.toFile($L))", opModel.getMethodName(),
                                       opModel.getInput().getVariableName(),
                                       ClassName.get(ResponseTransformer.class),
                                       "filePath")
                         .build();
    }

    /**
     * Generate a simple method for operations with streaming input and output members.
     * Streaming input member that reads data from a file and a streaming output member that write response content to a file.
     */
    private MethodSpec streamingInputOutputFileSimpleMethod(OperationModel opModel,
                                                            TypeName responseType,
                                                            ClassName requestType) {
        return MethodSpec.methodBuilder(opModel.getMethodName())
                         .returns(responseType)
                         .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                         .addParameter(requestType, opModel.getInput().getVariableName())
                         .addParameter(ClassName.get(Path.class), "sourcePath")
                         .addParameter(ClassName.get(Path.class), "destinationPath")
                         .addJavadoc(opModel.getDocs(model, ClientType.SYNC, SimpleMethodOverload.FILE))
                         .addExceptions(getExceptionClasses(model, opModel))
                         .addStatement("return $L($L, $T.fromFile(sourcePath), $T.toFile(destinationPath))",
                                       opModel.getMethodName(),
                                       opModel.getInput().getVariableName(),
                                       ClassName.get(RequestBody.class),
                                       ClassName.get(ResponseTransformer.class))
                         .build();
    }

    private static List<ClassName> getExceptionClasses(IntermediateModel model, OperationModel opModel) {
        List<ClassName> exceptions = opModel.getExceptions().stream()
                                            .map(e -> ClassName.get(model.getMetadata().getFullModelPackageName(),
                                                                    e.getExceptionName()))
                                            .collect(toCollection(ArrayList::new));
        Collections.addAll(exceptions, ClassName.get(AwsServiceException.class),
                           ClassName.get(SdkClientException.class),
                           ClassName.get(model.getMetadata().getFullModelPackageName(),
                                         model.getSdkModeledExceptionBaseClassName()));
        return exceptions;
    }

    private String consumerBuilderJavadoc(OperationModel opModel, SimpleMethodOverload overload) {
        return opModel.getDocs(model, ClientType.SYNC, overload, new DocConfiguration().isConsumerBuilder(true));
    }

    private MethodSpec utilitiesMethod() {
        UtilitiesMethod config = model.getCustomizationConfig().getUtilitiesMethod();
        ClassName returnType = PoetUtils.classNameFromFqcn(config.getReturnType());

        return MethodSpec.methodBuilder(UtilitiesMethod.METHOD_NAME)
                         .returns(returnType)
                         .addModifiers(Modifier.PUBLIC)
                         .addModifiers(Modifier.DEFAULT)
                         .addStatement("throw new $T()", UnsupportedOperationException.class)
                         .addJavadoc("Creates an instance of {@link $T} object with the "
                                     + "configuration set on this client.", returnType)
                         .build();
    }
}
