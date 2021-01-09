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

package software.amazon.awssdk.codegen.poet.paginators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.pagination.sync.PaginatedItemsIterable;
import software.amazon.awssdk.core.pagination.sync.PaginatedResponsesIterator;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.core.pagination.sync.SyncPageFetcher;

/**
 * Java poet {@link ClassSpec} to generate the response class for sync paginated operations.
 */
public class SyncResponseClassSpec extends PaginatorsClassSpec {

    protected static final String ITERATOR_METHOD = "iterator";

    public SyncResponseClassSpec(IntermediateModel model, String c2jOperationName, PaginatorDefinition paginatorDefinition) {
        super(model, c2jOperationName, paginatorDefinition);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder specBuilder = TypeSpec.classBuilder(className())
                                               .addModifiers(Modifier.PUBLIC)
                                               .addAnnotation(PoetUtils.generatedAnnotation())
                                               .addSuperinterface(getSyncResponseInterface())
                                               .addFields(fields().collect(Collectors.toList()))
                                               .addMethod(constructor())
                                               .addMethod(iteratorMethod())
                                               .addMethods(getMethodSpecsForResultKeyList())
                                               .addJavadoc(paginationDocs.getDocsForSyncResponseClass(
                                                   getClientInterfaceName()))
                                               .addType(nextPageFetcherClass().build());

        return specBuilder.build();
    }

    @Override
    public ClassName className() {
        return poetExtensions.getResponseClassForPaginatedSyncOperation(c2jOperationName);
    }

    /**
     * Returns the interface that is implemented by the Paginated Sync Response class.
     */
    private TypeName getSyncResponseInterface() {
        return ParameterizedTypeName.get(ClassName.get(SdkIterable.class), responseType());
    }

    /**
     * @return A Poet {@link ClassName} for the sync client interface
     */
    protected ClassName getClientInterfaceName() {
        return poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
    }

    protected Stream<FieldSpec> fields() {
        return Stream.of(syncClientInterfaceField(), requestClassField(), syncPageFetcherField());
    }

    protected FieldSpec syncClientInterfaceField() {
        return FieldSpec.builder(getClientInterfaceName(), CLIENT_MEMBER, Modifier.PRIVATE, Modifier.FINAL).build();
    }

    private FieldSpec syncPageFetcherField() {
        return FieldSpec.builder(SyncPageFetcher.class, NEXT_PAGE_FETCHER_MEMBER, Modifier.PRIVATE, Modifier.FINAL).build();
    }

    protected MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(getClientInterfaceName(), CLIENT_MEMBER)
                         .addParameter(requestType(), REQUEST_MEMBER)
                         .addStatement("this.$L = $L", CLIENT_MEMBER, CLIENT_MEMBER)
                         .addStatement("this.$L = $L", REQUEST_MEMBER, REQUEST_MEMBER)
                         .addStatement("this.$L = new $L()", NEXT_PAGE_FETCHER_MEMBER, nextPageFetcherClassName())
                .build();
    }

    /**
     * A {@link MethodSpec} for the overridden iterator() method which is inherited
     * from the interface.
     */
    protected MethodSpec iteratorMethod() {
        return MethodSpec.methodBuilder(ITERATOR_METHOD)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Iterator.class), responseType()))
                .addStatement("return $1T.builder().$2L($3L).build()", PaginatedResponsesIterator.class,
                              NEXT_PAGE_FETCHER_MEMBER, nextPageFetcherArgument())
                .build();
    }

    protected String nextPageFetcherArgument() {
        return NEXT_PAGE_FETCHER_MEMBER;
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
                                      .filter(Objects::nonNull)
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
     *      Function<DescribeFolderContentsResponse, Iterator<FolderMetadata>> getIterator = response -> {
     *          if (response != null && response.folders() != null) {
     *              return response.folders().iterator();
     *          }
     *          return Collections.emptyIterator();
     *      };
     *
     *      return PaginatedItemsIterable.builder().pagesIterable(this).itemIteratorFunction(getIterator).build();
     *  }
     */
    private MethodSpec getMethodsSpecForSingleResultKey(String resultKey) {
        MemberModel resultKeyModel = memberModelForResponseMember(resultKey);

        // TODO: Support other types besides List or Map
        if (!(resultKeyModel.isList() || resultKeyModel.isMap())) {
            return null;
        }

        TypeName resultKeyType = getTypeForResultKey(resultKey);

        return MethodSpec.methodBuilder(resultKeyModel.getFluentGetterMethodName())
                         .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                         .returns(ParameterizedTypeName.get(ClassName.get(SdkIterable.class), resultKeyType))
                         .addCode("$T getIterator = ",
                                  ParameterizedTypeName.get(ClassName.get(Function.class),
                                                            responseType(),
                                                            ParameterizedTypeName.get(ClassName.get(Iterator.class),
                                                                                      resultKeyType)))
                         .addCode(getIteratorLambdaBlock(resultKey, resultKeyModel))
                         .addCode("\n")
                         .addStatement("return $T.<$T, $T>builder().pagesIterable(this).itemIteratorFunction(getIterator).build"
                                       + "()",
                                       PaginatedItemsIterable.class, responseType(), resultKeyType)
                         .addJavadoc(CodeBlock.builder()
                                              .add("Returns an iterable to iterate through the paginated {@link $T#$L()} member."
                                                   + " The returned iterable is used to iterate through the results across all "
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

    /**
     * Generates a inner class that implements {@link SyncPageFetcher}. An instance of this class
     * is passed to {@link PaginatedResponsesIterator} to be used while iterating through pages.
     */
    protected TypeSpec.Builder nextPageFetcherClass() {
        return TypeSpec.classBuilder(nextPageFetcherClassName())
                       .addModifiers(Modifier.PRIVATE)
                       .addSuperinterface(ParameterizedTypeName.get(ClassName.get(SyncPageFetcher.class), responseType()))
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
                                            .build());
    }
}
