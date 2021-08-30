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

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getBatchRequestEntryType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getBatchRequestType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getBatchResponseType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getErrorBatchEntry;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getErrorEntriesMethod;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getRequestType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getResponseType;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getSuccessBatchEntry;
import static software.amazon.awssdk.codegen.poet.batchmanager.BatchTypesUtils.getType;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.codegen.model.config.customization.BatchManagerMethods;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.batchmanager.BatchAndSend;
import software.amazon.awssdk.core.batchmanager.BatchKeyMapper;
import software.amazon.awssdk.core.batchmanager.BatchResponseMapper;
import software.amazon.awssdk.core.batchmanager.IdentifiableMessage;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Logger;

public class BatchFunctionsClassSpec implements ClassSpec {

    private static final Logger log = Logger.loggerFor(BatchFunctionsClassSpec.class);

    private final IntermediateModel model;
    private final Map<String, BatchManagerMethods> batchFunctions;
    private final String modelPackage;
    private final PoetExtensions poetExtensions;
    private final ClassName className;
    private final ClassName clientName;
    private final ClassName asyncClientName;

    public BatchFunctionsClassSpec(IntermediateModel model) {
        this.model = model;
        this.batchFunctions = model.getCustomizationConfig().getBatchManagerMethods();
        this.modelPackage = model.getMetadata().getFullModelPackageName();
        this.poetExtensions = new PoetExtensions(model);
        this.className = poetExtensions.getBatchFunctionsClass();
        this.clientName = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
        this.asyncClientName = poetExtensions.getClientClass(model.getMetadata().getAsyncInterface());
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder classBuilder = PoetUtils.createClassBuilder(className)
                                                 .addAnnotation(SdkInternalApi.class)
                                                 .addModifiers(PUBLIC, FINAL)
                                                 .addMethod(constructor())
                                                 .addMethods(batchFunctions());

        return classBuilder.build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder().addModifiers(PRIVATE).build();
    }

    private List<MethodSpec> batchFunctions() {
        return batchFunctions.entrySet()
                             .stream()
                             .flatMap(this::safeBatchFunctions)
                             .collect(Collectors.toList());
    }

    private Stream<MethodSpec> safeBatchFunctions(Map.Entry<String, BatchManagerMethods> batchFunctions)
            throws IllegalArgumentException {
        try {
            return batchFunctions(batchFunctions);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Stream<MethodSpec> batchFunctions(Map.Entry<String, BatchManagerMethods> batchFunctions)
            throws IllegalArgumentException {
        List<MethodSpec> methods = new ArrayList<>();
        methods.addAll(batchingFunction(batchFunctions));
        methods.addAll(responseMapper(batchFunctions));
        methods.add(batchKeyMapper(batchFunctions));
        return methods.stream();
    }

    private List<MethodSpec> batchingFunction(Map.Entry<String, BatchManagerMethods> batchFunctions) {
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(batchingFunctionSync(batchFunctions));
        methods.add(batchingFunctionAsync(batchFunctions));
        methods.add(batchingFunctionHelper(batchFunctions));
        methods.add(addCreateBatchEntryMethod(batchFunctions));
        return methods;
    }

    private MethodSpec batchingFunctionSync(Map.Entry<String, BatchManagerMethods> batchFunctions) {
        String methodName = batchFunctions.getKey() + "BatchFunction";
        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, batchingFunctionReturn(batchFunctions))
            .addModifiers(PUBLIC, STATIC)
            .addParameter(clientName, "client")
            .addParameter(ClassName.get(Executor.class), "executor");
        addBatchFunctionStatementSync(builder, batchFunctions);

        return builder.build();
    }

    private MethodSpec batchingFunctionAsync(Map.Entry<String, BatchManagerMethods> batchFunctions) {
        String methodName = batchFunctions.getKey() + "BatchAsyncFunction";
        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, batchingFunctionReturn(batchFunctions))
            .addModifiers(PUBLIC, STATIC)
            .addParameter(asyncClientName, "client");
        addBatchFunctionStatementAsync(builder, batchFunctions);

        return builder.build();
    }

    private MethodSpec batchingFunctionHelper(Map.Entry<String, BatchManagerMethods> batchFunctions) {
        ClassName returnType = getBatchRequestType(batchFunctions, modelPackage);
        ClassName batchRequestEntryType = getBatchRequestEntryType(batchFunctions, modelPackage);
        ClassName batchRequestType = getBatchRequestType(batchFunctions, modelPackage);
        ParameterizedTypeName identifiedRequest = ParameterizedTypeName.get(ClassName.get(IdentifiableMessage.class),
                                                                            getRequestType(batchFunctions, modelPackage));
        ParameterizedTypeName requestsParam = ParameterizedTypeName.get(ClassName.get(List.class), identifiedRequest);
        String methodName = "create" + returnType.simpleName();

        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, returnType)
            .addModifiers(PRIVATE, STATIC)
            .addParameter(requestsParam, "identifiedRequests")
            .addParameter(ClassName.get(String.class), "batchKey");

        String batchKeyMethod = batchFunctions.getValue().getBatchKey();
        builder.addStatement("$T entries = identifiedRequests.stream()\n"
                             + ".map(identifiedRequest -> $N(identifiedRequest.id(),\n"
                             + "identifiedRequest.message()))\n"
                             + ".collect($T.toList())",
                             ParameterizedTypeName.get(ClassName.get(List.class), batchRequestEntryType),
                             addCreateBatchEntryMethod(batchFunctions),
                             ClassName.get(Collectors.class))
               .addCode("// Since requests are batched together according to a combination of their $L and "
                        + "overrideConfiguration, all requests must have the same overrideConfiguration so it is sufficient to "
                        + "retrieve it from the first request.\n", batchKeyMethod)
               .addStatement("$T overrideConfiguration = identifiedRequests\n"
                             + ".get(0)\n"
                             + ".message()\n"
                             + ".overrideConfiguration()",
                             ParameterizedTypeName.get(ClassName.get(Optional.class),
                                                       ClassName.get(AwsRequestOverrideConfiguration.class)))
               .addStatement("return overrideConfiguration.map(overrideConfig -> $T.builder()\n"
                             + ".$L(batchKey)\n"
                             + ".overrideConfiguration(overrideConfig)\n"
                             + ".entries(entries)\n"
                             + ".build())\n"
                             + ".orElse($T.builder()\n"
                             + ".$L(batchKey)\n"
                             + ".entries(entries)\n"
                             + ".build())",
                             batchRequestType, batchKeyMethod,
                             batchRequestType, batchKeyMethod);

        return builder.build();
    }

    private void addBatchFunctionStatementSync(MethodSpec.Builder builder,
                                               Map.Entry<String, BatchManagerMethods> batchFunctions) {
        addBatchFunctionStatementCore(builder, batchFunctions);
        builder.addStatement("    return $T.supplyAsync(() -> client.$L(batchRequest), executor);\n"
                             + "}",
                             ClassName.get(CompletableFuture.class),
                             batchFunctions.getValue().getBatchMethod());
    }

    private void addBatchFunctionStatementAsync(MethodSpec.Builder builder,
                                                Map.Entry<String, BatchManagerMethods> batchFunctions) {
        addBatchFunctionStatementCore(builder, batchFunctions);
        builder.addStatement("    return client.$L(batchRequest);\n"
                             + "}",
                             batchFunctions.getValue().getBatchMethod());
    }

    private void addBatchFunctionStatementCore(MethodSpec.Builder builder,
                                                      Map.Entry<String, BatchManagerMethods> batchFunctions) {
        builder.addStatement("return (identifiedRequests, batchKey) -> {\n"
                             + "    $T batchRequest = $N(identifiedRequests, batchKey)",
                             getBatchRequestType(batchFunctions, modelPackage),
                             batchingFunctionHelper(batchFunctions));
    }

    private MethodSpec addCreateBatchEntryMethod(Map.Entry<String, BatchManagerMethods> batchFunctions) {
        ClassName batchRequestEntryType = getBatchRequestEntryType(batchFunctions, modelPackage);
        ClassName requestType = getRequestType(batchFunctions, modelPackage);
        String methodName = "create" + batchRequestEntryType.simpleName();
        String requestParam = "request";

        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, batchRequestEntryType)
            .addModifiers(PRIVATE, STATIC)
            .addParameter(ClassName.get(String.class), "id")
            .addParameter(getRequestType(batchFunctions, modelPackage), requestParam);
        builder.addCode("return ");
        builder.addCode(builderMapOneClassToAnother(batchFunctions, batchRequestEntryType, requestType, requestParam));
        builder.addCode(".build();");

        return builder.build();
    }

    private ParameterizedTypeName batchingFunctionReturn(Map.Entry<String, BatchManagerMethods> batchFunctions) {
        ClassName requestClass = getRequestType(batchFunctions, modelPackage);
        ClassName batchResponseClass = getBatchResponseType(batchFunctions, modelPackage);

        return ParameterizedTypeName.get(ClassName.get(BatchAndSend.class),
                                         requestClass, batchResponseClass);
    }

    private List<MethodSpec> responseMapper(Map.Entry<String, BatchManagerMethods> batchFunctions)
            throws IllegalArgumentException {
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(responseMapperCore(batchFunctions));
        methods.add(createResponseFromEntry(batchFunctions));
        methods.add(createThrowableFromEntry(batchFunctions));
        return methods;
    }

    private MethodSpec responseMapperCore(Map.Entry<String, BatchManagerMethods> batchFunctions) throws IllegalArgumentException {
        BatchManagerMethods batchManagerMethods = batchFunctions.getValue();
        String methodName = batchFunctions.getKey() + "ResponseMapper";
        ClassName responseClass = getResponseType(batchFunctions, modelPackage);
        ClassName batchResponseClass = getBatchResponseType(batchFunctions, modelPackage);
        ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(BatchResponseMapper.class),
                                                                     batchResponseClass, responseClass);
        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, returnType)
            .addModifiers(PUBLIC, STATIC);

        ParameterizedTypeName identifiedResponse = ParameterizedTypeName.get(ClassName.get(IdentifiableMessage.class),
                                                                             responseClass);
        ParameterizedTypeName identifiedThrowable = ParameterizedTypeName.get(ClassName.get(IdentifiableMessage.class),
                                                                              ClassName.get(Throwable.class));
        ParameterizedTypeName either = ParameterizedTypeName.get(ClassName.get(Either.class), identifiedResponse,
                                                                 identifiedThrowable);
        ParameterizedTypeName mappedResponsesType = ParameterizedTypeName.get(ClassName.get(List.class), either);
        builder.addStatement("return batchResponse -> {\n"
                             + "    $T mappedResponses = new $T<>()",
                             mappedResponsesType, ClassName.get(ArrayList.class))
               .addStatement("batchResponse.$L()\n"
                             + " .forEach(batchResponseEntry -> {\n"
                             + "    IdentifiableMessage<$T> response = $N(batchResponseEntry, batchResponse);\n"
                             + "    mappedResponses.add(Either.left(response));\n"
                             + "})",
                             batchManagerMethods.getSuccessEntriesMethod(), responseClass,
                             createResponseFromEntry(batchFunctions));

        String errorEntriesMethod = getErrorEntriesMethod(batchFunctions);
        String errorCodeMethod = batchManagerMethods.getErrorCodeMethod();
        if (errorEntriesMethod.equals(batchManagerMethods.getSuccessEntriesMethod())) {
            throw new IllegalArgumentException("A batch operation must return a separate list for success and errors in the "
                                               + "response");
        } else {
            if (errorCodeMethod != null) {
                builder.addStatement("batchResponse.$L()\n"
                                     + ".forEach(batchResponseEntry -> {\n"
                                     + "    IdentifiableMessage<Throwable> response = $N(batchResponseEntry);\n"
                                     + "    mappedResponses.add(Either.right(response));\n"
                                     + "})",
                                     getErrorEntriesMethod(batchFunctions),
                                     createThrowableFromEntry(batchFunctions));
            }
        }

        builder.addStatement("return mappedResponses;\n"
                             + "}");

        return builder.build();
    }

    private MethodSpec createResponseFromEntry(Map.Entry<String, BatchManagerMethods> batchFunctions) {
        String methodName = "create" + getResponseType(batchFunctions, modelPackage).simpleName();
        String responseParam = "successfulEntry";
        ClassName responseClass = getResponseType(batchFunctions, modelPackage);
        ClassName responseEntryClass = getSuccessBatchEntry(batchFunctions, modelPackage);
        ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(IdentifiableMessage.class), responseClass);
        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, returnType)
            .addModifiers(PRIVATE, STATIC)
            .addParameter(responseEntryClass, responseParam)
            .addParameter(getBatchResponseType(batchFunctions, modelPackage), "batchResponse");

        builder.addStatement("String key = successfulEntry.$L()", batchFunctions.getValue().getBatchRequestIdentifier());
        builder.addCode("$T.Builder builder = ", responseClass);
        builder.addStatement(builderMapOneClassToAnother(batchFunctions, responseClass, responseEntryClass, responseParam));
        builder.beginControlFlow("if (batchResponse.responseMetadata() != null)")
               .addStatement("builder.responseMetadata(batchResponse.responseMetadata())")
               .endControlFlow();
        builder.beginControlFlow("if (batchResponse.sdkHttpResponse() != null)")
               .addStatement("builder.sdkHttpResponse(batchResponse.sdkHttpResponse())")
               .endControlFlow();
        builder.addStatement("$T response = builder.build()", responseClass);
        builder.addStatement("return new $T(key, response)",
                             returnType);

        return builder.build();
    }

    private MethodSpec createThrowableFromEntry(Map.Entry<String, BatchManagerMethods> batchFunctions) {
        String methodName = batchFunctions.getKey() + "CreateThrowable";
        String responseParam = "failedEntry";
        BatchManagerMethods batchManagerMethods = batchFunctions.getValue();
        ClassName responseClass = ClassName.get(Throwable.class);
        ClassName exceptionType = getType(model.getMetadata().getServiceName() + "Exception", modelPackage);
        ParameterizedTypeName returnClass = ParameterizedTypeName.get(ClassName.get(IdentifiableMessage.class), responseClass);
        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, returnClass)
            .addModifiers(PRIVATE, STATIC)
            .addParameter(getErrorBatchEntry(batchFunctions, modelPackage), responseParam);

        // TODO: Figure out a better way to return error entry's. Right now we are just returning the error code and message in
        //  an exception but ideally we would want to return all field's in the error entry.
        builder.addStatement("String key = failedEntry.$L()", batchManagerMethods.getBatchRequestIdentifier());

        builder.addCode("$T errorDetailsBuilder = $T.builder()",
                        ClassName.get(AwsErrorDetails.class), ClassName.get(AwsErrorDetails.class));
        if (batchManagerMethods.getErrorCodeMethod() != null) {
            builder.addCode(".errorCode($L.$L())", responseParam, batchManagerMethods.getErrorCodeMethod());
        }
        if (batchManagerMethods.getErrorMessageMethod() != null) {
            builder.addCode(".errorMessage($L.$L())", responseParam, batchManagerMethods.getErrorMessageMethod());
        }
        builder.addCode(".build();\n");

        builder.addStatement("$T response = $T.builder().awsErrorDetails(errorDetailsBuilder).build()",
                             responseClass, exceptionType);
        builder.addStatement("return new $T(key, response)",
                             returnClass);

        return builder.build();
    }

    private CodeBlock builderMapOneClassToAnother(Map.Entry<String, BatchManagerMethods> batchFunctions, ClassName newType,
                                                  ClassName originalType, String originalParam) {
        CodeBlock.Builder builder = CodeBlock.builder();

        builder.add("$T.builder()", newType);
        ShapeModel newShape = model.getShapes().get(newType.simpleName());
        ShapeModel originalShape = model.getShapes().get(originalType.simpleName());

        if (newShape == null) {
            throw new IllegalArgumentException("Bad input for type: " + newType.simpleName());
        }
        if (originalShape == null) {
            throw new IllegalArgumentException("Bad input for type: " + originalType.simpleName());
        }

        newShape.getMembers()
                .forEach(memberModel -> {
                    MemberModel foundMember = originalShape.getMemberByName(memberModel.getName());
                    String setterMethod = memberModel.getFluentSetterMethodName();
                    if (setterMethod.equals(batchFunctions.getValue().getBatchRequestIdentifier())) {
                        builder.add(".$L(id)\n", setterMethod);
                    } else if (foundMember != null) {
                        String getterMethod = foundMember.getFluentGetterMethodName();
                        builder.add(".$L($L.$L())\n", setterMethod, originalParam, getterMethod);
                    } else {
                        log.debug(() -> originalType.simpleName() + " doesn't have method: " + memberModel.getName());
                    }
                });

        return builder.build();
    }

    private MethodSpec batchKeyMapper(Map.Entry<String, BatchManagerMethods> batchFunctions) {
        String methodName = batchFunctions.getKey() + "BatchKeyMapper";
        String batchKeyMethod = batchFunctions.getValue().getBatchKey();
        ClassName requestClass = getRequestType(batchFunctions, modelPackage);
        ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(BatchKeyMapper.class), requestClass);

        MethodSpec.Builder builder = methodSignatureWithReturnType(methodName, returnType)
            .addModifiers(PUBLIC, STATIC);
        builder.addStatement("return request -> request.overrideConfiguration()\n"
                             + ".map(overrideConfig -> request.$L()  + overrideConfig.hashCode())\n"
                             + ".orElse(request.$L())",
                             batchKeyMethod, batchKeyMethod);

        return builder.build();
    }

    private MethodSpec.Builder methodSignatureWithReturnType(String methodName, TypeName returnType) {
        return MethodSpec.methodBuilder(methodName)
                         .returns(returnType);
    }

}
