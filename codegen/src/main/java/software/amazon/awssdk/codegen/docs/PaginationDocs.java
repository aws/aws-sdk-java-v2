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

package software.amazon.awssdk.codegen.docs;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.utils.PaginatorUtils;
import software.amazon.awssdk.core.internal.async.SequentialSubscriber;

public class PaginationDocs {

    private static final String SUBSCRIBE_METHOD_NAME = "subscribe";

    private final OperationModel operationModel;
    private final PoetExtensions poetExtensions;

    public PaginationDocs(IntermediateModel intermediateModel, OperationModel operationModel) {
        this.operationModel = operationModel;
        this.poetExtensions = new PoetExtensions(intermediateModel);
    }

    /**
     * Constructs additional documentation on the client operation that is appended to the service documentation.
     *
     * TODO Add a link to our developer guide which will have more details and sample code. Write a blog post too.
     */
    public String getDocsForSyncOperation() {
        return CodeBlock.builder()
                        .add("<p>This is a variant of {@link #$L($T)} operation. "
                             + "The return type is a custom iterable that can be used to iterate through all the pages. "
                             + "SDK will internally handle making service calls for you.\n</p>",
                             operationModel.getMethodName(), requestType())
                        .add("<p>\nWhen this operation is called, a custom iterable is returned but no service calls are "
                             + "made yet. So there is no guarantee that the request is valid. As you iterate "
                             + "through the iterable, SDK will start lazily loading response pages by making service calls until "
                             + "there are no pages left or your iteration stops. If there are errors in your request, you will "
                             + "see the failures only after you start iterating through the iterable.\n</p>")
                        .add(getSyncCodeSnippets())
                        .build()
                        .toString();
    }

    /**
     * Constructs javadocs for the generated response classes in a paginated operation.
     * @param clientInterface A java poet {@link ClassName} type of the sync client interface
     */
    public String getDocsForSyncResponseClass(ClassName clientInterface) {
        return CodeBlock.builder()
                        .add("<p>Represents the output for the {@link $T#$L($T)} operation which is a paginated operation."
                             + " This class is an iterable of {@link $T} that can be used to iterate through all the "
                             + "response pages of the operation.</p>",
                             clientInterface, getPaginatedMethodName(), requestType(), syncResponsePageType())
                        .add("<p>When the operation is called, an instance of this class is returned.  At this point, "
                             + "no service calls are made yet and so there is no guarantee that the request is valid. "
                             + "As you iterate through the iterable, SDK will start lazily loading response pages by making "
                             + "service calls until there are no pages left or your iteration stops. If there are errors in your "
                             + "request, you will see the failures only after you start iterating through the iterable.</p>")
                        .add(getSyncCodeSnippets())
                        .build()
                        .toString();
    }

    /**
     * Constructs additional documentation on the async client operation that is appended to the service documentation.
     */
    public String getDocsForAsyncOperation() {
        return CodeBlock.builder()
                        .add("<p>This is a variant of {@link #$L($T)} operation. "
                             + "The return type is a custom publisher that can be subscribed to request a stream of response "
                             + "pages. SDK will internally handle making service calls for you.\n</p>",
                             operationModel.getMethodName(), requestType())
                        .add("<p>When the operation is called, an instance of this class is returned.  At this point, "
                             + "no service calls are made yet and so there is no guarantee that the request is valid. "
                             + "If there are errors in your request, you will see the failures only after you start streaming "
                             + "the data. The subscribe method should be called as a request to start streaming data. "
                             + "For more info, see {@link $T#$L($T)}. Each call to the subscribe method will result in a new "
                             + "{@link $T} i.e., a new contract to stream data from the starting request.</p>",
                             getPublisherType(), SUBSCRIBE_METHOD_NAME, getSubscriberType(), getSubscriptionType())
                        .add(getAsyncCodeSnippets())
                        .build()
                        .toString();
    }

    /**
     * Constructs javadocs for the generated response classes of a paginated operation in Async client.
     * @param clientInterface A java poet {@link ClassName} type of the Async client interface
     */
    public String getDocsForAsyncResponseClass(ClassName clientInterface) {
        return CodeBlock.builder()
                        .add("<p>Represents the output for the {@link $T#$L($T)} operation which is a paginated operation."
                             + " This class is a type of {@link $T} which can be used to provide a sequence of {@link $T} "
                             + "response pages as per demand from the subscriber.</p>",
                             clientInterface, getPaginatedMethodName(), requestType(), getPublisherType(),
                             syncResponsePageType())
                        .add("<p>When the operation is called, an instance of this class is returned.  At this point, "
                             + "no service calls are made yet and so there is no guarantee that the request is valid. "
                             + "If there are errors in your request, you will see the failures only after you start streaming "
                             + "the data. The subscribe method should be called as a request to start streaming data. "
                             + "For more info, see {@link $T#$L($T)}. Each call to the subscribe method will result in a new "
                             + "{@link $T} i.e., a new contract to stream data from the starting request.</p>",
                             getPublisherType(), SUBSCRIBE_METHOD_NAME, getSubscriberType(), getSubscriptionType())
                        .add(getAsyncCodeSnippets())
                        .build()
                        .toString();
    }

    private String getSyncCodeSnippets() {
        CodeBlock callOperationOnClient = CodeBlock.builder()
                                           .addStatement("$T responses = client.$L(request)", syncPaginatedResponseType(),
                                                         getPaginatedMethodName())
                                           .build();

        return CodeBlock.builder()
                        .add("\n\n<p>The following are few ways to iterate through the response pages:</p>")
                        .add("1) Using a Stream")
                        .add(buildCode(CodeBlock.builder()
                                                .add(callOperationOnClient)
                                                .addStatement("responses.stream().forEach(....)")
                                                .build()))
                        .add("\n\n2) Using For loop")
                        .add(buildCode(CodeBlock.builder()
                                                .add(callOperationOnClient)
                                                .beginControlFlow("for ($T response : responses)", syncResponsePageType())
                                                .addStatement(" // do something")
                                                .endControlFlow()
                                                .build()))
                        .add("\n\n3) Use iterator directly")
                        .add(buildCode(CodeBlock.builder()
                                                .add(callOperationOnClient)
                                                .addStatement("responses.iterator().forEachRemaining(....)")
                                                .build()))
                        .add(noteAboutSyncNonPaginatedMethod())
                        .build()
                        .toString();
    }

    private String getAsyncCodeSnippets() {
        CodeBlock callOperationOnClient = CodeBlock.builder()
                                                   .addStatement("$T publisher = client.$L(request)",
                                                                 asyncPaginatedResponseType(),
                                                                 getPaginatedMethodName())
                                                   .build();

        return CodeBlock.builder()
                        .add("\n\n<p>The following are few ways to use the response class:</p>")
                        .add("1) Using the forEach helper method",
                             TypeName.get(SequentialSubscriber.class))
                        .add(buildCode(CodeBlock.builder()
                                                .add(callOperationOnClient)
                                                .add(CodeBlock.builder()
                                                              .addStatement("CompletableFuture<Void> future = publisher"
                                                                            + ".forEach(res -> "
                                                                            + "{ // Do something with the response })")
                                                              .addStatement("future.get()")
                                                              .build())
                                                .build()))
                        .add("\n\n2) Using a custom subscriber")
                        .add(buildCode(CodeBlock.builder()
                                                .add(callOperationOnClient)
                                                .add("publisher.subscribe(new Subscriber<$T>() {\n\n", syncResponsePageType())
                                                .addStatement("public void onSubscribe($T subscription) { //... }",
                                                              getSubscriberType())
                                                .add("\n\n")
                                                .addStatement("public void onNext($T response) { //... }", syncResponsePageType())
                                                .add("});")
                                                .build()))
                        .add("As the response is a publisher, it can work well with third party reactive streams implementations "
                             + "like RxJava2.")
                        .add(noteAboutSyncNonPaginatedMethod())
                        .build()
                        .toString();
    }

    private CodeBlock buildCode(CodeBlock codeSnippet) {
        return CodeBlock.builder()
                        .add("<pre>{@code\n")
                        .add(codeSnippet)
                        .add("}</pre>")
                        .build();
    }

    /**
     * @return Method name for the sync paginated operation
     */
    private String getPaginatedMethodName() {
        return PaginatorUtils.getPaginatedMethodName(operationModel.getMethodName());
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
     * @return A Poet {@link ClassName} for the return type of sync non-paginated operation.
     *
     * Example: For ListTables operation, it will be "ListTablesResponse" class.
     */
    private ClassName syncResponsePageType() {
        return poetExtensions.getModelClass(operationModel.getReturnType().getReturnType());
    }

    /**
     * @return A Poet {@link ClassName} for the return type of sync paginated operation.
     */
    private ClassName syncPaginatedResponseType() {
        return poetExtensions.getResponseClassForPaginatedSyncOperation(operationModel.getOperationName());
    }

    /**
     * @return A Poet {@link ClassName} for the return type of Async paginated operation.
     */
    private ClassName asyncPaginatedResponseType() {
        return poetExtensions.getResponseClassForPaginatedAsyncOperation(operationModel.getOperationName());
    }

    private CodeBlock noteAboutSyncNonPaginatedMethod() {
        return CodeBlock.builder()
                        .add("\n<p><b>Note: If you prefer to have control on service calls, use the {@link #$L($T)} operation."
                             + "</b></p>", operationModel.getMethodName(), requestType())
                        .build();
    }

    /**
     * @return A Poet {@link ClassName} for the reactive streams {@link Publisher}.
     */
    private ClassName getPublisherType() {
        return ClassName.get(Publisher.class);
    }

    /**
     * @return A Poet {@link ClassName} for the reactive streams {@link Subscriber}.
     */
    private ClassName getSubscriberType() {
        return ClassName.get(Subscriber.class);
    }

    /**
     * @return A Poet {@link ClassName} for the reactive streams {@link Subscription}.
     */
    private ClassName getSubscriptionType() {
        return ClassName.get(Subscription.class);
    }
}
