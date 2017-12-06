/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet.paginators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.docs.PaginationDocs;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.model.TypeProvider;
import software.amazon.awssdk.core.pagination.NextPageFetcher;
import software.amazon.awssdk.core.pagination.PaginatedItemsIterable;
import software.amazon.awssdk.core.pagination.PaginatedResponsesIterator;
import software.amazon.awssdk.core.pagination.SdkIterable;

/**
 * Java poet {@link ClassSpec} to generate the response class for sync paginated operations.
 */
public class PaginatorResponseClassSpec implements ClassSpec {

    private static final Logger log = LoggerFactory.getLogger(PaginatorResponseClassSpec.class);

    private static final String CLIENT_MEMBER = "client";
    private static final String REQUEST_MEMBER = "firstRequest";
    private static final String NEXT_PAGE_FETCHER_MEMBER = "nextPageFetcher";
    private static final String HAS_NEXT_PAGE_METHOD = "hasNextPage";
    private static final String NEXT_PAGE_METHOD = "nextPage";
    private static final String PREVIOUS_PAGE_METHOD_ARGUMENT = "previousPage";

    private final IntermediateModel model;
    private final PoetExtensions poetExtensions;
    private final TypeProvider typeProvider;
    private final String c2jOperationName;
    private final PaginatorDefinition paginatorDefinition;
    private final OperationModel operationModel;
    private final PaginationDocs paginationDocs;

    public PaginatorResponseClassSpec(IntermediateModel intermediateModel,
                                      String c2jOperationName,
                                      PaginatorDefinition paginatorDefinition) {
        this.model = intermediateModel;
        this.poetExtensions = new PoetExtensions(intermediateModel);
        this.typeProvider = new TypeProvider(intermediateModel);
        this.c2jOperationName = c2jOperationName;
        this.paginatorDefinition = paginatorDefinition;
        this.operationModel = model.getOperation(c2jOperationName);
        this.paginationDocs = new PaginationDocs(intermediateModel, operationModel);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder specBuilder = TypeSpec.classBuilder(className())
                                               .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                               .addAnnotation(PoetUtils.GENERATED)
                                               .addSuperinterface(getPaginatedResponseInterface())
                                               .addFields(Stream.of(syncClientInterfaceField(),
                                                                    requestClassField(),
                                                                    nextPageSupplierField())
                                                                .collect(Collectors.toList()))
                                               .addMethod(constructor())
                                               .addMethod(iteratorMethod())
                                               .addMethods(getMethodSpecsForResultKeyList())
                                               .addJavadoc(paginationDocs.getDocsForSyncResponseClass(
                                                   getClientInterfaceName()))
                                               .addType(nextPageFetcherClass());

        return specBuilder.build();
    }

    @Override
    public ClassName className() {
        return poetExtensions.getResponseClassForPaginatedSyncOperation(c2jOperationName);
    }

    /**
     * Returns the interface that is implemented by the Paginated Response class.
     */
    private TypeName getPaginatedResponseInterface() {
        return ParameterizedTypeName.get(ClassName.get(SdkIterable.class), responseType());
    }

    /**
     * @return A Poet {@link ClassName} for the sync operation request type.
     *
     * Example: For ListTables operation, it will be "ListTablesRequest" class.
     */
    private ClassName requestType() {
        return poetExtensions.getModelClass(operationModel.getInput().getVariableType());
    }

    /**
     * @return A Poet {@link ClassName} for the sync operation response type.
     *
     * Example: For ListTables operation, it will be "ListTablesResponse" class.
     */
    private ClassName responseType() {
        return poetExtensions.getModelClass(operationModel.getReturnType().getReturnType());
    }

    /**
     * @return A Poet {@link ClassName} for the sync client interface
     */
    private ClassName getClientInterfaceName() {
        return poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
    }

    private FieldSpec syncClientInterfaceField() {
        return FieldSpec.builder(getClientInterfaceName(), CLIENT_MEMBER, Modifier.PRIVATE, Modifier.FINAL).build();
    }

    private FieldSpec requestClassField() {
        return FieldSpec.builder(requestType(), REQUEST_MEMBER, Modifier.PRIVATE, Modifier.FINAL).build();
    }

    private FieldSpec nextPageSupplierField() {
        return FieldSpec.builder(NextPageFetcher.class, NEXT_PAGE_FETCHER_MEMBER, Modifier.PRIVATE, Modifier.FINAL).build();
    }

    private String nextPageFetcherClassName() {
        return operationModel.getReturnType().getReturnType() + "Fetcher";
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(getClientInterfaceName(), CLIENT_MEMBER, Modifier.FINAL)
                         .addParameter(requestType(), REQUEST_MEMBER, Modifier.FINAL)
                         .addStatement("this.$L = $L", CLIENT_MEMBER, CLIENT_MEMBER)
                         .addStatement("this.$L = $L", REQUEST_MEMBER, REQUEST_MEMBER)
                         .addStatement("this.$L = new $L()", NEXT_PAGE_FETCHER_MEMBER, nextPageFetcherClassName())
                .build();
    }

    /**
     * A {@link MethodSpec} for the overridden iterator() method which is inherited
     * from the interface.
     */
    private MethodSpec iteratorMethod() {
        return MethodSpec.methodBuilder("iterator")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Iterator.class), responseType()))
                .addStatement("return new $T($L)", PaginatedResponsesIterator.class, NEXT_PAGE_FETCHER_MEMBER)
                .build();
    }

    /**
     * Returns iterable of {@link MethodSpec} to generate helper methods for all members
     * in {@link PaginatorDefinition#getResultKey()}. All the generated methods return an SdkIterable.
     *
     * The helper methods to iterate on paginated member will be generated only
     * if {@link PaginatorDefinition#getResultKey()} is not null and a non-empty list.
     */
    private Iterable<MethodSpec> getMethodSpecsForResultKeyList() {
        if (paginatorDefinition.getResultKey() != null) {
            return paginatorDefinition.getResultKey().stream()
                                      .map(this::getMethodsSpecForSingleResultKey)
                                      .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /*
     * Generate a method spec for single element in {@link PaginatorDefinition#getResultKey()} list.
     *
     * If the element is "Folders" and its type is "List<FolderMetadata>", generated code looks like:
     *
     *  public SdkIterable<FolderMetadata> folders() {
     *      Function<DescribeFolderContentsResponse, Iterator<FolderMetadata>> getPaginatedMemberIterator =
     *              response -> response != null ? response.folders().iterator() : null;
     *
     *      return new PaginatedItemsIterable(this, getPaginatedMemberIterator);
     *  }
     */
    private MethodSpec getMethodsSpecForSingleResultKey(String resultKey) {
        TypeName resultKeyType = getTypeForResultKey(resultKey);
        MemberModel resultKeyModel = memberModelForResponseMember(resultKey);

        return MethodSpec.methodBuilder(resultKeyModel.getFluentGetterMethodName())
                         .addModifiers(Modifier.PUBLIC)
                         .returns(ParameterizedTypeName.get(ClassName.get(SdkIterable.class), resultKeyType))
                         .addCode("$T getIterator = ",
                                  ParameterizedTypeName.get(ClassName.get(Function.class),
                                                            responseType(),
                                                            ParameterizedTypeName.get(ClassName.get(Iterator.class),
                                                                                      resultKeyType)))
                         .addCode(getPaginatedMemberIteratorLambdaBlock(resultKey, resultKeyModel))
                         .addCode("\n")
                         .addStatement("return new $T(this, getIterator)", PaginatedItemsIterable.class)
                         .addJavadoc(CodeBlock.builder()
                                              .add("Returns an iterable to iterate through the paginated {@link $T#$L()} member. "
                                                   + "The returned iterable is used to iterate through the results across all "
                                                   + "response pages and not a single page.\n",
                                                   responseType(), resultKeyModel.getFluentGetterMethodName())
                                              .add("\n")
                                              .add("This method is useful if you are interested in iterating over the paginated "
                                                   + "member in the response pages instead of the top level pages. "
                                                   + "Similar to iteration over pages, this method internally makes service "
                                                   + "calls to get the next list of results until the iteration stops or "
                                                   + "there are no more results.")
                                              .build())
                         .build();
    }

    private CodeBlock getPaginatedMemberIteratorLambdaBlock(String resultKey, MemberModel resultKeyModel) {
        final String response = "response";
        final String fluentGetter = fluentGetterMethodForResponseMember(resultKey);

        CodeBlock iteratorBlock = null;

        if (resultKeyModel.isList()) {
            iteratorBlock = CodeBlock.builder().add("$L.$L.iterator()", response, fluentGetter).build();

        } else if (resultKeyModel.isMap()) {
            iteratorBlock = CodeBlock.builder().add("$L.$L.entrySet().iterator()", response, fluentGetter).build();
        }

        return CodeBlock.builder()
                        .addStatement("$L -> $L != null ? $L : null", response, response, iteratorBlock)
                        .build();
    }

    /**
     * Returns a list of fluent setter method names for members in {@link PaginatorDefinition#getInputToken()} list.
     * The size of list returned by this method is equal to the size of {@link PaginatorDefinition#getInputToken()} list.
     */
    private List<String> fluentSetterMethodNamesForInputToken() {
        return paginatorDefinition.getInputToken().stream()
                                  .map(this::fluentSetterNameForSingleInputToken)
                                  .collect(Collectors.toList());
    }

    /**
     * Returns the fluent setter method name for a single member in the request.
     *
     * The values in {@link PaginatorDefinition#getInputToken()} are not nested unlike
     * {@link PaginatorDefinition#getOutputToken()}.
     */
    private String fluentSetterNameForSingleInputToken(String inputToken) {
        return operationModel.getInputShape()
                .findMemberModelByC2jName(inputToken)
                .getFluentSetterMethodName();
    }

    /**
     * Returns a list of fluent getter methods for members in {@link PaginatorDefinition#getOutputToken()} list.
     * The size of list returned by this method is equal to the size of {@link PaginatorDefinition#getOutputToken()} list.
     */
    private List<String> fluentGetterMethodsForOutputToken() {
        return paginatorDefinition.getOutputToken().stream()
                                  .map(this::fluentGetterMethodForResponseMember)
                                  .collect(Collectors.toList());
    }

    /**
     * Returns the fluent getter method for a single member in the response.
     * The returned String includes the '()' after each method name.
     *
     * The input member can be a nested String. An example would be StreamDescription.LastEvaluatedShardId
     * which represents LastEvaluatedShardId member in StreamDescription class. The return value for it
     * would be "streamDescription().lastEvaluatedShardId()"
     *
     * @param member A top level or nested member in response of {@link #c2jOperationName}.
     */
    private String fluentGetterMethodForResponseMember(String member) {
        final String[] hierarchy = member.split("\\.");

        if (hierarchy.length < 1) {
            throw new IllegalArgumentException(String.format("Error when splitting member %s for operation %s",
                                                             member, c2jOperationName));
        }

        ShapeModel parentShape = operationModel.getOutputShape();
        final StringBuilder getterMethod = new StringBuilder();

        for (String str : hierarchy) {
            getterMethod.append(".")
                    .append(parentShape.findMemberModelByC2jName(str).getFluentGetterMethodName())
                    .append("()");

            parentShape =  parentShape.findMemberModelByC2jName(str).getShape();
        }

        return getterMethod.substring(1);
    }

    /**
     * @param input A top level or nested member in response of {@link #c2jOperationName}.
     *
     * @return The {@link MemberModel} of the {@link PaginatorDefinition#getResultKey()}. If input value is nested,
     * then member model of the last child shape is returned.
     *
     * For example, if input is StreamDescription.Shards, then the return value is "Shard" which is the member model for
     * the Shards.
     */
    private MemberModel memberModelForResponseMember(String input) {
        final String[] hierarchy = input.split("\\.");

        if (hierarchy.length < 1) {
            throw new IllegalArgumentException(String.format("Error when splitting value %s for operation %s",
                                                             input, c2jOperationName));
        }

        ShapeModel shape = operationModel.getOutputShape();

        for (int i = 0; i < hierarchy.length - 1; i++) {
            shape = shape.findMemberModelByC2jName(hierarchy[i]).getShape();
        }

        return shape.getMemberByC2jName(hierarchy[hierarchy.length - 1]);
    }

    /*
     * Returns the {@link TypeName} for a value in the {@link PaginatorDefinition#getResultKey()} list.
     *
     * Examples:
     * If paginated item is represented as List<String>, then member type is String.
     * If paginated item is represented as List<Foo>, then member type is Foo.
     * If paginated item is represented as Map<String, List<Foo>>,
     *              then member type is Map.Entry<String, List<Foo>>.
     */
    private TypeName getTypeForResultKey(String singleResultKey) {
        MemberModel resultKeyModel = memberModelForResponseMember(singleResultKey);

        if (resultKeyModel == null) {
            throw new InvalidParameterException("MemberModel is not found for result key: " + singleResultKey);
        }

        if (resultKeyModel.isList()) {
            return typeProvider.fieldType(resultKeyModel.getListModel().getListMemberModel());
        } else if (resultKeyModel.isMap()) {
            return typeProvider.mapEntryWithConcreteTypes(resultKeyModel.getMapModel());
        } else {
            throw new IllegalArgumentException(String.format("Key %s in paginated operation %s should be either a list or a map",
                                                             singleResultKey, c2jOperationName));
        }
    }

    /**
     * Generates a inner class that implements {@link NextPageFetcher}. An instance of this class
     * is passed to {@link PaginatedResponsesIterator} to be used while iterating through pages.
     */
    private TypeSpec nextPageFetcherClass() {
        return TypeSpec.classBuilder(nextPageFetcherClassName())
                       .addModifiers(Modifier.PRIVATE)
                       .addSuperinterface(ParameterizedTypeName.get(ClassName.get(NextPageFetcher.class), responseType()))
                       .addMethod(MethodSpec.methodBuilder(HAS_NEXT_PAGE_METHOD)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addAnnotation(Override.class)
                                            .addParameter(responseType(), PREVIOUS_PAGE_METHOD_ARGUMENT)
                                            .returns(boolean.class)
                                            .addStatement(hasNextPageMethodBody())
                                            .build())
                       .addMethod(MethodSpec.methodBuilder(NEXT_PAGE_METHOD)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addAnnotation(Override.class)
                                            .addParameter(responseType(), PREVIOUS_PAGE_METHOD_ARGUMENT)
                                            .returns(responseType())
                                            .addCode(nextPageMethodBody())
                                            .build())
                       .build();
    }

    private String hasNextPageMethodBody() {
        String body;

        if (paginatorDefinition.getMoreResults() != null) {
            body = String.format("return %s.%s.booleanValue()",
                                 PREVIOUS_PAGE_METHOD_ARGUMENT,
                                 fluentGetterMethodForResponseMember(paginatorDefinition.getMoreResults()));
        } else {
            // If there is no more_results token, then output_token will be a single value
            body = String.format("return %s.%s != null",
                                 PREVIOUS_PAGE_METHOD_ARGUMENT,
                                 fluentGetterMethodsForOutputToken().get(0));
        }

        return body;
    }

    /*
     * Returns {@link CodeBlock} for the NEXT_PAGE_METHOD.
     *
     * A sample from dynamoDB listTables paginator:
     *
     *  if (oldPage == null) {
     *      return client.listTables(firstRequest);
     *  } else {
     *      return client.listTables(firstRequest.toBuilder().exclusiveStartTableName(response.lastEvaluatedTableName())
     *                               .build());
     *  }
     */
    private CodeBlock nextPageMethodBody() {
        return CodeBlock.builder()
                        .beginControlFlow("if ($L == null)", PREVIOUS_PAGE_METHOD_ARGUMENT)
                        .addStatement("return $L.$L($L)", CLIENT_MEMBER, operationModel.getMethodName(), REQUEST_MEMBER)
                        .endControlFlow()
                        .addStatement(codeToGetNextPageIfOldResponseIsNotNull())
                        .build();
    }

    /**
     * Generates the code to get next page by using values from old page.
     *
     * Sample generated code:
     * return client.listTables(firstRequest.toBuilder().exclusiveStartTableName(response.lastEvaluatedTableName()).build());
     */
    private String codeToGetNextPageIfOldResponseIsNotNull() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("return %s.%s(%s.toBuilder()", CLIENT_MEMBER, operationModel.getMethodName(), REQUEST_MEMBER));

        List<String> requestSetterNames = fluentSetterMethodNamesForInputToken();
        List<String> responseGetterMethods = fluentGetterMethodsForOutputToken();

        for (int i = 0; i < paginatorDefinition.getInputToken().size(); i++) {
            sb.append(String.format(".%s(%s.%s)", requestSetterNames.get(i), PREVIOUS_PAGE_METHOD_ARGUMENT,
                                    responseGetterMethods.get(i)));
        }

        sb.append(".build())");

        return sb.toString();
    }
}
