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

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;

/**
 * Implementations of {@link OperationDocProvider} for async client methods. This implementation is for the typical
 * async method (i.e. on that takes a request object and returns a {@link java.util.concurrent.CompletableFuture} of the response
 * object). Subclasses provide documentation for specialized method overloads like simple no-arg methods.
 */
class AsyncOperationDocProvider extends OperationDocProvider {

    private static final String REQUEST_BODY_DOCS =
            "Functional interface that can be implemented to produce the request content " +
            "in a non-blocking manner. The size of the content is expected to be known up front. " +
            "See {@link AsyncRequestBody} for specific details on implementing this interface as well " +
            "as links to precanned implementations for common scenarios like uploading from a file. ";

    private static final String STREAM_RESPONSE_TRANSFORMER_DOCS =
            "The response transformer for processing the streaming response in a " +
            "non-blocking manner. See {@link AsyncResponseTransformer} for details on how this callback " +
            "should be implemented and for links to precanned implementations for common scenarios like " +
            "downloading to a file. ";

    AsyncOperationDocProvider(IntermediateModel model, OperationModel opModel, DocConfiguration configuration) {
        super(model, opModel, configuration);
    }

    @Override
    protected String getDefaultServiceDocs() {
        return String.format("Invokes the %s operation asynchronously.", opModel.getOperationName());
    }

    @Override
    protected String getInterfaceName() {
        return model.getMetadata().getAsyncInterface();
    }

    @Override
    protected void applyReturns(DocumentationBuilder docBuilder) {
        if (opModel.hasStreamingOutput()) {
            docBuilder.returns("A future to the transformed result of the AsyncResponseTransformer.");
        } else {
            docBuilder.returns("A Java Future containing the result of the %s operation returned by the service.",
                               opModel.getOperationName());
        }
    }


    @Override
    protected void applyParams(DocumentationBuilder docBuilder) {
        emitRequestParm(docBuilder);
        if (opModel.hasStreamingInput()) {
            docBuilder.param("requestBody", REQUEST_BODY_DOCS + getStreamingInputDocs());
        }
        if (opModel.hasStreamingOutput()) {
            docBuilder.param("asyncResponseTransformer", STREAM_RESPONSE_TRANSFORMER_DOCS + getStreamingOutputDocs());
        }
    }

    @Override
    protected void applyThrows(DocumentationBuilder docBuilder) {
        docBuilder.asyncThrows(getThrows());
    }

    /**
     * Provider for streaming simple methods that take a file (to either upload from for streaming inputs or download to for
     * streaming outputs).
     */
    static class AsyncFile extends AsyncOperationDocProvider {

        AsyncFile(IntermediateModel model, OperationModel opModel, DocConfiguration configuration) {
            super(model, opModel, configuration);
        }

        @Override
        protected void applyParams(DocumentationBuilder docBuilder) {
            emitRequestParm(docBuilder);
            if (opModel.hasStreamingInput()) {
                docBuilder.param("path", SIMPLE_FILE_INPUT_DOCS + getStreamingInputDocs());

            }
            if (opModel.hasStreamingOutput()) {
                docBuilder.param("path", SIMPLE_FILE_OUTPUT_DOCS + getStreamingOutputDocs());
            }
        }
    }

    /**
     * Provider for simple method that takes no arguments and creates an empty request object.
     */
    static class AsyncNoArg extends AsyncOperationDocProvider {

        AsyncNoArg(IntermediateModel model, OperationModel opModel, DocConfiguration configuration) {
            super(model, opModel, configuration);
        }

        @Override
        protected void applyParams(DocumentationBuilder docBuilder) {
        }
    }

    /**
     * Provider for traditional paginated method that takes in a request object and returns a response object.
     */
    static class AsyncPaginated extends AsyncOperationDocProvider {

        AsyncPaginated(IntermediateModel model, OperationModel opModel, DocConfiguration configuration) {
            super(model, opModel, configuration);
        }

        @Override
        protected String appendToDescription() {
            return paginationDocs.getDocsForAsyncOperation();
        }

        @Override
        protected void applyReturns(DocumentationBuilder docBuilder) {
            docBuilder.returns("A custom publisher that can be subscribed to request a stream of response pages.");
        }
    }

    /**
     * Provider for paginated simple method that takes no arguments and creates an empty request object.
     */
    static class AsyncPaginatedNoArg extends AsyncPaginated {

        AsyncPaginatedNoArg(IntermediateModel model, OperationModel opModel, DocConfiguration configuration) {
            super(model, opModel, configuration);
        }

        @Override
        protected void applyParams(DocumentationBuilder docBuilder) {
        }
    }
}
