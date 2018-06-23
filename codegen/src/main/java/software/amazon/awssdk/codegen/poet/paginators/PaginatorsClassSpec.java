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

package software.amazon.awssdk.codegen.poet.paginators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.docs.PaginationDocs;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.model.TypeProvider;

public abstract class PaginatorsClassSpec implements ClassSpec {

    protected static final String CLIENT_MEMBER = "client";
    protected static final String REQUEST_MEMBER = "firstRequest";
    protected static final String NEXT_PAGE_FETCHER_MEMBER = "nextPageFetcher";
    protected static final String HAS_NEXT_PAGE_METHOD = "hasNextPage";
    protected static final String NEXT_PAGE_METHOD = "nextPage";
    protected static final String RESUME_METHOD = "resume";
    protected static final String PREVIOUS_PAGE_METHOD_ARGUMENT = "previousPage";
    protected static final String RESPONSE_LITERAL = "response";
    protected static final String LAST_SUCCESSFUL_PAGE_LITERAL = "lastSuccessfulPage";

    protected final IntermediateModel model;
    protected final String c2jOperationName;
    protected final PaginatorDefinition paginatorDefinition;
    protected final PoetExtensions poetExtensions;
    protected final TypeProvider typeProvider;
    protected final OperationModel operationModel;
    protected final PaginationDocs paginationDocs;

    public PaginatorsClassSpec(IntermediateModel model, String c2jOperationName, PaginatorDefinition paginatorDefinition) {
        this.model = model;
        this.c2jOperationName = c2jOperationName;
        this.paginatorDefinition = paginatorDefinition;
        this.poetExtensions = new PoetExtensions(model);
        this.typeProvider = new TypeProvider(model);
        this.operationModel = model.getOperation(c2jOperationName);
        this.paginationDocs = new PaginationDocs(model, operationModel);
    }

    /**
     * @return A Poet {@link ClassName} for the operation request type.
     *
     * Example: For ListTables operation, it will be "ListTablesRequest" class.
     */
    protected ClassName requestType() {
        return poetExtensions.getModelClass(operationModel.getInput().getVariableType());
    }

    /**
     * @return A Poet {@link ClassName} for the sync operation response type.
     *
     * Example: For ListTables operation, it will be "ListTablesResponse" class.
     */
    protected ClassName responseType() {
        return poetExtensions.getModelClass(operationModel.getReturnType().getReturnType());
    }

    // Generates
    // private final ListTablesRequest firstRequest;
    protected FieldSpec requestClassField() {
        return FieldSpec.builder(requestType(), REQUEST_MEMBER, Modifier.PRIVATE, Modifier.FINAL).build();
    }

    protected String nextPageFetcherClassName() {
        return operationModel.getReturnType().getReturnType() + "Fetcher";
    }

    protected MethodSpec.Builder resumeMethodBuilder() {
        return MethodSpec.methodBuilder(RESUME_METHOD)
                         .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                         .addParameter(responseType(), LAST_SUCCESSFUL_PAGE_LITERAL)
                         .returns(className())
                         .addCode(CodeBlock.builder()
                                           .beginControlFlow("if ($L.$L($L))", NEXT_PAGE_FETCHER_MEMBER,
                                                             HAS_NEXT_PAGE_METHOD, LAST_SUCCESSFUL_PAGE_LITERAL)
                                           .addStatement("return new $T($L, $L)", className(), CLIENT_MEMBER,
                                                         constructRequestFromLastPage(LAST_SUCCESSFUL_PAGE_LITERAL))
                                           .endControlFlow()
                                           .build())
                         .addJavadoc(CodeBlock.builder()
                                              .add("<p>A helper method to resume the pages in case of unexpected failures. "
                                                   + "The method takes the last successful response page as input and returns an "
                                                   + "instance of {@link $T} that can be used to retrieve the consecutive pages "
                                                   + "that follows the input page.</p>", className())
                                              .build());
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
    protected TypeName getTypeForResultKey(String singleResultKey) {
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
     * @param input A top level or nested member in response of {@link #c2jOperationName}.
     *
     * @return The {@link MemberModel} of the {@link PaginatorDefinition#getResultKey()}. If input value is nested,
     * then member model of the last child shape is returned.
     *
     * For example, if input is StreamDescription.Shards, then the return value is "Shard" which is the member model for
     * the Shards.
     */
    protected MemberModel memberModelForResponseMember(String input) {
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

    protected String hasNextPageMethodBody() {
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
    protected CodeBlock nextPageMethodBody() {
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
        sb.append(String.format("return %s.%s(%s)", CLIENT_MEMBER,
                                operationModel.getMethodName(),
                                constructRequestFromLastPage(PREVIOUS_PAGE_METHOD_ARGUMENT)));
        return sb.toString();
    }

    /**
     * Generates the code to construct a request object from the last successful page
     * by setting the fields required to get the next page.
     *
     * Sample code: if responsePage string is "response"
     * firstRequest.toBuilder().exclusiveStartTableName(response.lastEvaluatedTableName()).build()
     */
    protected String constructRequestFromLastPage(String responsePage) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s.toBuilder()", REQUEST_MEMBER));

        List<String> requestSetterNames = fluentSetterMethodNamesForInputToken();
        List<String> responseGetterMethods = fluentGetterMethodsForOutputToken();

        for (int i = 0; i < paginatorDefinition.getInputToken().size(); i++) {
            sb.append(String.format(".%s(%s.%s)", requestSetterNames.get(i), responsePage,
                                    responseGetterMethods.get(i)));
        }

        sb.append(".build()");
        return sb.toString();
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

    protected CodeBlock getIteratorLambdaBlock(String resultKey, MemberModel resultKeyModel) {
        final String conditionalStatement = getConditionalStatementforIteratorLambda(resultKey);
        final String fluentGetter = fluentGetterMethodForResponseMember(resultKey);

        CodeBlock iteratorBlock = null;
        if (resultKeyModel.isList()) {
            iteratorBlock = CodeBlock.builder().addStatement("return $L.$L.iterator()", RESPONSE_LITERAL, fluentGetter).build();

        } else if (resultKeyModel.isMap()) {
            iteratorBlock = CodeBlock.builder().addStatement("return $L.$L.entrySet().iterator()",
                                                             RESPONSE_LITERAL,
                                                             fluentGetter).build();
        }

        CodeBlock conditionalBlock = CodeBlock.builder()
                                              .beginControlFlow("if ($L)", conditionalStatement)
                                              .add(iteratorBlock)
                                              .endControlFlow()
                                              .addStatement("return $T.emptyIterator()", TypeName.get(Collections.class))
                                              .build();

        return CodeBlock.builder()
                        .add("$L -> { $L };", RESPONSE_LITERAL, conditionalBlock)
                        .build();
    }

    /**
     * Returns a conditional statement string that verifies the fluent methods to return result key are not null.
     *
     * If resultKey is StreamDescription.LastEvaluatedShardId, output of this method would be
     * "response != null && response.streamDescription() != null && response.streamDescription().lastEvaluatedShardId() != null"
     *
     * @param resultKey A top level or nested member in response of {@link #c2jOperationName}.
     */
    private String getConditionalStatementforIteratorLambda(String resultKey) {
        final String[] hierarchy = resultKey.split("\\.");

        if (hierarchy.length < 1) {
            throw new IllegalArgumentException(String.format("Error when splitting member %s for operation %s",
                                                             resultKey, c2jOperationName));
        }

        String currentFluentMethod = RESPONSE_LITERAL;
        ShapeModel parentShape = operationModel.getOutputShape();

        final StringBuilder conditionStatement = new StringBuilder(String.format("%s != null", currentFluentMethod));

        for (String str : hierarchy) {
            currentFluentMethod = String.format("%s.%s()", currentFluentMethod, parentShape.findMemberModelByC2jName(str)
                                                                                           .getFluentGetterMethodName());
            conditionStatement.append(" && ");
            conditionStatement.append(String.format("%s != null", currentFluentMethod));

            parentShape =  parentShape.findMemberModelByC2jName(str).getShape();
        }

        return conditionStatement.toString();
    }
}