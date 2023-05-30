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

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static software.amazon.awssdk.codegen.internal.Constant.SYNC_CLIENT_DESTINATION_PATH_PARAM_NAME;
import static software.amazon.awssdk.codegen.internal.Constant.SYNC_CLIENT_SOURCE_PATH_PARAM_NAME;
import static software.amazon.awssdk.codegen.internal.Constant.SYNC_STREAMING_INPUT_PARAM;
import static software.amazon.awssdk.codegen.internal.Constant.SYNC_STREAMING_OUTPUT_PARAM;
import static software.amazon.awssdk.codegen.poet.client.AsyncClientInterface.STREAMING_TYPE_VARIABLE;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.codegen.docs.ClientType;
import software.amazon.awssdk.codegen.docs.DocConfiguration;
import software.amazon.awssdk.codegen.docs.SimpleMethodOverload;
import software.amazon.awssdk.codegen.docs.WaiterDocs;
import software.amazon.awssdk.codegen.model.config.customization.UtilitiesMethod;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.model.DeprecationUtils;
import software.amazon.awssdk.codegen.utils.PaginatorUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.regions.ServiceMetadataProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

public class SyncClientInterface implements ClassSpec {

    private final IntermediateModel model;
    private final ClassName className;
    private final String clientPackageName;
    private final PoetExtension poetExtensions;

    public SyncClientInterface(IntermediateModel model) {
        this.model = model;
        this.clientPackageName = model.getMetadata().getFullClientPackageName();
        this.className = ClassName.get(clientPackageName, model.getMetadata().getSyncInterface());
        this.poetExtensions = new PoetExtension(model);
    }

    @Override
    public final TypeSpec poetSpec() {
        TypeSpec.Builder result = createTypeSpec();
        addInterfaceClass(result);
        addAnnotations(result);
        addModifiers(result);
        addFields(result);
        result.addMethods(operations());
        if (model.getCustomizationConfig().getUtilitiesMethod() != null) {
            result.addMethod(utilitiesMethod());
        }
        if (model.hasWaiters()) {
            result.addMethod(waiterMethod());
        }
        addAdditionalMethods(result);
        addCloseMethod(result);
        return result.build();
    }

    protected void addInterfaceClass(TypeSpec.Builder type) {
        type.addSuperinterface(SdkClient.class);
    }

    protected TypeSpec.Builder createTypeSpec() {
        return PoetUtils.createInterfaceBuilder(className);
    }

    protected void addAnnotations(TypeSpec.Builder type) {
        type.addAnnotation(SdkPublicApi.class)
            .addAnnotation(ThreadSafe.class);
    }

    protected void addModifiers(TypeSpec.Builder type) {
    }

    protected void addCloseMethod(TypeSpec.Builder type) {
    }

    protected void addFields(TypeSpec.Builder type) {
        type.addField(FieldSpec.builder(String.class, "SERVICE_NAME")
                               .addModifiers(PUBLIC, STATIC, FINAL)
                               .initializer("$S", model.getMetadata().getSigningName())
                               .build())
            .addField(FieldSpec.builder(String.class, "SERVICE_METADATA_ID")
                               .addModifiers(PUBLIC, STATIC, FINAL)
                               .initializer("$S", model.getMetadata().getEndpointPrefix())
                               .addJavadoc("Value for looking up the service's metadata from the {@link $T}.",
                                           ServiceMetadataProvider.class)
                               .build());
    }

    protected void addAdditionalMethods(TypeSpec.Builder type) {

        if (!model.getCustomizationConfig().isExcludeClientCreateMethod()) {
            type.addMethod(create());
        }

        type.addMethod(builder())
            .addMethod(serviceMetadata());

        PoetUtils.addJavadoc(type::addJavadoc, getJavadoc());
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
                         .addModifiers(STATIC, PUBLIC)
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
                         .addModifiers(STATIC, PUBLIC)
                         .addJavadoc("Create a builder that can be used to configure and create a {@link $T}.", className)
                         .addStatement("return new $T()", builderClass)
                         .build();
    }

    protected Iterable<MethodSpec> operations() {
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
                         .addModifiers(STATIC, PUBLIC)
                         .addStatement("return $T.of(SERVICE_METADATA_ID)", ServiceMetadata.class)
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

        return methods.stream()
                      // Add Deprecated annotation if needed to all overloads
                      .map(m -> DeprecationUtils.checkDeprecated(opModel, m))
                      .collect(toList());
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
                                                           .addModifiers(PUBLIC)
                                                           .addJavadoc(opModel.getDocs(model, ClientType.SYNC,
                                                                                       simpleMethodOverload))
                                                           .addExceptions(getExceptionClasses(model, opModel));

        addFirstParameter.accept(methodBuilder);
        streamingMethod(methodBuilder, opModel, responseType);

        return methodBuilder;
    }

    private List<MethodSpec> operation(OperationModel opModel) {
        List<MethodSpec> methods = new ArrayList<>();

        MethodSpec.Builder builder = operationMethodSignature(model, opModel);
        MethodSpec method = operationBody(builder, opModel).build();
        methods.add(method);

        addConsumerMethod(methods, method, SimpleMethodOverload.NORMAL, opModel);

        return methods;
    }

    protected MethodSpec.Builder operationBody(MethodSpec.Builder builder, OperationModel opModel) {
        return builder.addModifiers(DEFAULT)
                      .addStatement("throw new $T()", UnsupportedOperationException.class);
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

        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                                               .returns(returnType)
                                               .addModifiers(PUBLIC)
                                               .addExceptions(getExceptionClasses(model, opModel));

        return simpleMethodModifier(builder);
    }

    protected List<MethodSpec> paginatedMethods(OperationModel opModel) {
        List<MethodSpec> paginatedMethodSpecs = new ArrayList<>();

        if (opModel.isPaginated()) {
            if (opModel.getInputShape().isSimpleMethod()) {
                paginatedMethodSpecs.add(paginatedSimpleMethod(opModel));
            }

            MethodSpec.Builder paginatedMethodBuilder =
                    operationMethodSignature(model,
                                             opModel,
                                             SimpleMethodOverload.PAGINATED,
                                             PaginatorUtils.getPaginatedMethodName(opModel.getMethodName()))
                            .returns(poetExtensions.getResponseClassForPaginatedSyncOperation(opModel.getOperationName()));
            MethodSpec paginatedMethod = paginatedMethodBody(paginatedMethodBuilder, opModel).build();
            paginatedMethodSpecs.add(paginatedMethod);

            addConsumerMethod(paginatedMethodSpecs, paginatedMethod, SimpleMethodOverload.PAGINATED, opModel);
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

    protected MethodSpec.Builder paginatedMethodBody(MethodSpec.Builder builder, OperationModel operationModel) {
        return builder.addModifiers(DEFAULT)
                      .addStatement("throw new $T()", UnsupportedOperationException.class);
    }

    private static void streamingMethod(MethodSpec.Builder methodBuilder, OperationModel opModel, TypeName responseType) {
        if (opModel.hasStreamingInput()) {
            methodBuilder.addParameter(ClassName.get(RequestBody.class), SYNC_STREAMING_INPUT_PARAM);
        }
        if (opModel.hasStreamingOutput()) {
            methodBuilder.addTypeVariable(STREAMING_TYPE_VARIABLE);
            ParameterizedTypeName streamingResponseHandlerType = ParameterizedTypeName
                    .get(ClassName.get(ResponseTransformer.class), responseType, STREAMING_TYPE_VARIABLE);
            methodBuilder.addParameter(streamingResponseHandlerType, SYNC_STREAMING_OUTPUT_PARAM);
        }
    }

    private List<MethodSpec> streamingSimpleMethods(OperationModel opModel) {
        TypeName responseType = ClassName.get(model.getMetadata().getFullModelPackageName(),
                                              opModel.getReturnType().getReturnType());
        ClassName requestType = ClassName.get(model.getMetadata().getFullModelPackageName(),
                                              opModel.getInput().getVariableType());

        List<MethodSpec> simpleMethods = new ArrayList<>();

        if (opModel.hasStreamingInput() && opModel.hasStreamingOutput()) {
            MethodSpec simpleMethod = streamingInputOutputFileSimpleMethod(opModel, responseType, requestType);
            simpleMethods.add(simpleMethod);
            addConsumerMethod(simpleMethods, simpleMethod, SimpleMethodOverload.FILE, opModel);

        } else if (opModel.hasStreamingInput()) {
            MethodSpec simpleMethod = uploadFromFileSimpleMethod(opModel, responseType, requestType);
            simpleMethods.add(simpleMethod);
            addConsumerMethod(simpleMethods, simpleMethod, SimpleMethodOverload.FILE, opModel);

        } else if (opModel.hasStreamingOutput()) {
            MethodSpec downloadToFileSimpleMethod = downloadToFileSimpleMethod(opModel, responseType, requestType);
            MethodSpec inputStreamSimpleMethod = inputStreamSimpleMethod(opModel, responseType, requestType);
            MethodSpec bytesSimpleMethod = bytesSimpleMethod(opModel, responseType, requestType);

            simpleMethods.add(downloadToFileSimpleMethod);
            addConsumerMethod(simpleMethods, downloadToFileSimpleMethod, SimpleMethodOverload.FILE, opModel);
            simpleMethods.add(inputStreamSimpleMethod);
            addConsumerMethod(simpleMethods, inputStreamSimpleMethod, SimpleMethodOverload.INPUT_STREAM, opModel);
            simpleMethods.add(bytesSimpleMethod);
            addConsumerMethod(simpleMethods, bytesSimpleMethod, SimpleMethodOverload.BYTES, opModel);
        }

        return simpleMethods;
    }

    protected void addConsumerMethod(List<MethodSpec> specs, MethodSpec spec, SimpleMethodOverload overload,
                                     OperationModel opModel) {
        String fileConsumerBuilderJavadoc = consumerBuilderJavadoc(opModel, overload);
        specs.add(ClientClassUtils.consumerBuilderVariant(spec, fileConsumerBuilderJavadoc));
    }

    /**
     * @return Simple method for streaming input operations to read data from a file.
     */
    private MethodSpec uploadFromFileSimpleMethod(OperationModel opModel, TypeName responseType, ClassName requestType) {
        String methodName = opModel.getMethodName();
        ParameterSpec inputVarParam = ParameterSpec.builder(requestType, opModel.getInput().getVariableName()).build();
        ParameterSpec srcPathParam = ParameterSpec.builder(ClassName.get(Path.class),
                                                              SYNC_CLIENT_SOURCE_PATH_PARAM_NAME).build();
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                                               .returns(responseType)
                                               .addModifiers(PUBLIC)
                                               .addParameter(inputVarParam)
                                               .addParameter(srcPathParam)
                                               .addJavadoc(opModel.getDocs(model, ClientType.SYNC, SimpleMethodOverload.FILE))
                                               .addExceptions(getExceptionClasses(model, opModel))
                                               .addStatement("return $L($N, $T.fromFile($N))", methodName,
                                                             inputVarParam,
                                                             ClassName.get(RequestBody.class),
                                                             srcPathParam);

        return simpleMethodModifier(builder).build();
    }

    /**
     * @return Simple method for streaming output operations to get content as an input stream.
     */
    private MethodSpec inputStreamSimpleMethod(OperationModel opModel, TypeName responseType, ClassName requestType) {
        TypeName returnType = ParameterizedTypeName.get(ClassName.get(ResponseInputStream.class), responseType);
        MethodSpec.Builder builder = MethodSpec.methodBuilder(opModel.getMethodName())
                                               .returns(returnType)
                                               .addModifiers(PUBLIC)
                                               .addParameter(requestType, opModel.getInput().getVariableName())
                                               .addJavadoc(opModel.getDocs(model, ClientType.SYNC,
                                                                           SimpleMethodOverload.INPUT_STREAM))
                                               .addExceptions(getExceptionClasses(model, opModel))
                                               .addStatement("return $L($L, $T.toInputStream())", opModel.getMethodName(),
                                                             opModel.getInput().getVariableName(),
                                                             ClassName.get(ResponseTransformer.class));

        return simpleMethodModifier(builder).build();
    }

    /**
     * @return Simple method for streaming output operations to get the content as a byte buffer or other in-memory types.
     */
    private MethodSpec bytesSimpleMethod(OperationModel opModel, TypeName responseType, ClassName requestType) {
        TypeName returnType = ParameterizedTypeName.get(ClassName.get(ResponseBytes.class), responseType);
        MethodSpec.Builder builder = MethodSpec.methodBuilder(opModel.getMethodName() + "AsBytes")
                                               .returns(returnType)
                                               .addModifiers(PUBLIC)
                                               .addParameter(requestType, opModel.getInput().getVariableName())
                                               .addJavadoc(opModel.getDocs(model, ClientType.SYNC, SimpleMethodOverload.BYTES))
                                               .addExceptions(getExceptionClasses(model, opModel))
                                               .addStatement("return $L($L, $T.toBytes())", opModel.getMethodName(),
                                                             opModel.getInput().getVariableName(),
                                                             ClassName.get(ResponseTransformer.class));

        return simpleMethodModifier(builder).build();
    }

    /**
     * @return Simple method for streaming output operations to write response content to a file.
     */
    private MethodSpec downloadToFileSimpleMethod(OperationModel opModel, TypeName responseType, ClassName requestType) {
        String methodName = opModel.getMethodName();
        ParameterSpec inputVarParam = ParameterSpec.builder(requestType, opModel.getInput().getVariableName()).build();
        ParameterSpec dstFileParam =
            ParameterSpec.builder(ClassName.get(Path.class), SYNC_CLIENT_DESTINATION_PATH_PARAM_NAME).build();
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                                               .returns(responseType)
                                               .addModifiers(PUBLIC)
                                               .addParameter(inputVarParam)
                                               .addParameter(dstFileParam)
                                               .addJavadoc(opModel.getDocs(model, ClientType.SYNC, SimpleMethodOverload.FILE))
                                               .addExceptions(getExceptionClasses(model, opModel))
                                               .addStatement("return $L($N, $T.toFile($N))", methodName,
                                                             inputVarParam,
                                                             ClassName.get(ResponseTransformer.class),
                                                             dstFileParam);

        return simpleMethodModifier(builder).build();
    }

    /**
     * Generate a simple method for operations with streaming input and output members.
     * Streaming input member that reads data from a file and a streaming output member that write response content to a file.
     */
    private MethodSpec streamingInputOutputFileSimpleMethod(OperationModel opModel,
                                                            TypeName responseType,
                                                            ClassName requestType) {
        String methodName = opModel.getMethodName();
        ParameterSpec inputVarParam = ParameterSpec.builder(requestType, opModel.getInput().getVariableName()).build();
        ParameterSpec srcFileParam = ParameterSpec.builder(ClassName.get(Path.class), SYNC_CLIENT_SOURCE_PATH_PARAM_NAME).build();
        ParameterSpec dstFileParam =
            ParameterSpec.builder(ClassName.get(Path.class), SYNC_CLIENT_DESTINATION_PATH_PARAM_NAME).build();
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                         .returns(responseType)
                         .addModifiers(PUBLIC)
                         .addParameter(inputVarParam)
                         .addParameter(srcFileParam)
                         .addParameter(dstFileParam)
                         .addJavadoc(opModel.getDocs(model, ClientType.SYNC, SimpleMethodOverload.FILE))
                         .addExceptions(getExceptionClasses(model, opModel))
                         .addStatement("return $L($N, $T.fromFile($N), $T.toFile($N))",
                                       methodName,
                                       inputVarParam,
                                       ClassName.get(RequestBody.class), srcFileParam,
                                       ClassName.get(ResponseTransformer.class),
                                       dstFileParam);

        return simpleMethodModifier(builder).build();
    }

    protected MethodSpec.Builder simpleMethodModifier(MethodSpec.Builder builder) {
        return builder.addModifiers(DEFAULT);
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

    protected MethodSpec utilitiesMethod() {
        UtilitiesMethod config = model.getCustomizationConfig().getUtilitiesMethod();
        ClassName returnType = PoetUtils.classNameFromFqcn(config.getReturnType());

        MethodSpec.Builder builder = MethodSpec.methodBuilder(UtilitiesMethod.METHOD_NAME)
                                               .returns(returnType)
                                               .addModifiers(PUBLIC)
                                               .addJavadoc("Creates an instance of {@link $T} object with the "
                                                           + "configuration set on this client.", returnType);

        return utilitiesOperationBody(builder).build();
    }

    protected MethodSpec.Builder utilitiesOperationBody(MethodSpec.Builder builder) {
        return builder.addModifiers(DEFAULT).addStatement("throw new $T()", UnsupportedOperationException.class);
    }

    protected MethodSpec waiterMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("waiter")
                                               .addModifiers(PUBLIC)
                                               .returns(poetExtensions.getSyncWaiterInterface())
                                               .addJavadoc(WaiterDocs.waiterMethodInClient(poetExtensions
                                                                                               .getSyncWaiterInterface()));

        return waiterOperationBody(builder).build();
    }

    protected MethodSpec.Builder waiterOperationBody(MethodSpec.Builder builder) {
        return builder.addModifiers(DEFAULT, PUBLIC)
                      .addStatement("throw new $T()", UnsupportedOperationException.class);
    }
}
