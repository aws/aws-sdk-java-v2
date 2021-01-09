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

package software.amazon.awssdk.codegen.poet.paginators.customizations;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.awssdk.codegen.poet.paginators.SyncResponseClassSpec;
import software.amazon.awssdk.core.util.PaginatorUtils;

/**
 * Customized response class spec for sync paginated operations that indicate the
 * last page by returning the same token passed in the request object.
 *
 * See Cloudwatch logs GetLogEvents API for example,
 * https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_GetLogEvents.html
 */
public class SameTokenSyncResponseClassSpec extends SyncResponseClassSpec {

    private static final String LAST_TOKEN_MEMBER = "lastToken";

    public SameTokenSyncResponseClassSpec(IntermediateModel model, String c2jOperationName,
                                          PaginatorDefinition paginatorDefinition) {
        super(model, c2jOperationName, paginatorDefinition);
    }

    @Override
    protected Stream<FieldSpec> fields() {
        return Stream.of(syncClientInterfaceField(), requestClassField());
    }

    @Override
    protected MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(getClientInterfaceName(), CLIENT_MEMBER)
                         .addParameter(requestType(), REQUEST_MEMBER)
                         .addStatement("this.$L = $L", CLIENT_MEMBER, CLIENT_MEMBER)
                         .addStatement("this.$L = $L", REQUEST_MEMBER, REQUEST_MEMBER)
                         .build();
    }

    @Override
    protected String nextPageFetcherArgument() {
        return String.format("new %s()", nextPageFetcherClassName());
    }

    @Override
    protected TypeSpec.Builder nextPageFetcherClass() {
        return super.nextPageFetcherClass()
                    .addField(FieldSpec.builder(Object.class, LAST_TOKEN_MEMBER, Modifier.PRIVATE)
                                       .build());
    }

    @Override
    protected CodeBlock hasNextPageMethodBody() {
        if (paginatorDefinition.getMoreResults() != null) {
            return CodeBlock.builder()
                            .add("return $N.$L.booleanValue()",
                                 PREVIOUS_PAGE_METHOD_ARGUMENT,
                                 fluentGetterMethodForResponseMember(paginatorDefinition.getMoreResults()))
                            .build();
        }
        // If there is no more_results token, then output_token will be a single value
        return CodeBlock.builder()
                        .add("return $3T.isOutputTokenAvailable($1N.$2L) && ",
                             PREVIOUS_PAGE_METHOD_ARGUMENT,
                             fluentGetterMethodsForOutputToken().get(0),
                             PaginatorUtils.class)
                        .add("!$1N.$2L.equals($3L)",
                             PREVIOUS_PAGE_METHOD_ARGUMENT,
                             fluentGetterMethodsForOutputToken().get(0),
                             LAST_TOKEN_MEMBER)
                        .build();
    }

    @Override
    protected CodeBlock nextPageMethodBody() {
        return CodeBlock.builder()
                        .beginControlFlow("if ($L == null)", PREVIOUS_PAGE_METHOD_ARGUMENT)
                        .addStatement("$L = null", LAST_TOKEN_MEMBER)
                        .addStatement("return $L.$L($L)", CLIENT_MEMBER, operationModel.getMethodName(), REQUEST_MEMBER)
                        .endControlFlow()
                        .addStatement("$1L = $2L.$3L", LAST_TOKEN_MEMBER, PREVIOUS_PAGE_METHOD_ARGUMENT,
                             fluentGetterMethodsForOutputToken().get(0))
                        .addStatement(codeToGetNextPageIfOldResponseIsNotNull())
                        .build();
    }
}
