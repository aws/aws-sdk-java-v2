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

package software.amazon.awssdk.codegen.docs;

import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.core.util.ImmutableMapParameter;

/**
 * Implementations of {@link OperationDocProvider} for async client methods. This implementation is for the typical
 * async method (i.e. on that takes a request object and returns a {@link java.util.concurrent.CompletableFuture} of the response
 * object). Subclasses provide documentation for specialized method overloads like simple no-arg methods.
 */
class AsyncOperationDocProvider extends OperationDocProvider {

    private static final String REQUEST_PROVIDER_DOCS =
            "Functional interface that can be implemented to produce the request content " +
            "in a non-blocking manner. The size of the content is expected to be known up front. " +
            "See {@link AsyncRequestProvider} for specific details on implementing this interface as well " +
            "as links to precanned implementations for common scenarios like uploading from a file. ";

    private static final String STREAM_RESPONSE_HANDLER_DOCS =
            "The response handler for processing the streaming response in a " +
            "non-blocking manner. See {@link AsyncResponseHandler} for details on how this callback " +
            "should be implemented and for links to precanned implementations for common scenarios like " +
            "downloading to a file. ";

    private AsyncOperationDocProvider(IntermediateModel model, OperationModel opModel) {
        super(model, opModel);
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
            docBuilder.returns("A future to the transformed result of the AsyncResponseHandler.");
        } else {
            docBuilder.returns("A Java Future containing the result of the %s operation returned by the service.",
                               opModel.getOperationName());
        }
    }


    @Override
    protected void applyParams(DocumentationBuilder docBuilder) {
        emitRequestParm(docBuilder);
        if (opModel.hasStreamingInput()) {
            docBuilder.param("requestProvider", REQUEST_PROVIDER_DOCS + getStreamingInputDocs());
        }
        if (opModel.hasStreamingOutput()) {
            docBuilder.param("asyncResponseHandler", STREAM_RESPONSE_HANDLER_DOCS + getStreamingOutputDocs());
        }
    }

    @Override
    protected void applyThrows(DocumentationBuilder docBuilder) {
        docBuilder.asyncThrows(getThrows());
    }

    /**
     * Note that {@link SimpleMethodOverload#INPUT_STREAM} does not make sense for Async and is not generated.
     *
     * @return Factories to use for the {@link ClientType#ASYNC} method type.
     */
    static Map<SimpleMethodOverload, Factory> asyncFactories() {
        return ImmutableMapParameter.of(SimpleMethodOverload.NORMAL, AsyncOperationDocProvider::new,
                                        SimpleMethodOverload.NO_ARG, AsyncNoArg::new,
                                        SimpleMethodOverload.FILE, AsyncFile::new,
                                        SimpleMethodOverload.CONSUMER_BUILDER, AsyncConsumerBuilder::new);
    }

    /**
     * Provider for streaming simple methods that take a file (to either upload from for streaming inputs or download to for
     * streaming outputs).
     */
    private static class AsyncFile extends AsyncOperationDocProvider {

        private AsyncFile(IntermediateModel model, OperationModel opModel) {
            super(model, opModel);
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
    private static class AsyncNoArg extends AsyncOperationDocProvider {

        private AsyncNoArg(IntermediateModel model, OperationModel opModel) {
            super(model, opModel);
        }

        @Override
        protected void applyParams(DocumentationBuilder docBuilder) {
        }
    }

    private static class AsyncConsumerBuilder extends AsyncOperationDocProvider {
        private AsyncConsumerBuilder(IntermediateModel model, OperationModel opModel) {
            super(model, opModel);
        }

        @Override
        protected String appendToDescription() {
            return "This is a convenience which creates an instance of the {@link " +
                   opModel.getInput().getSimpleType() +
                   ".Builder} avoiding the need to create one manually via {@link " +
                   opModel.getInput().getSimpleType() +
                   "#builder()}";
        }

        @Override
        protected void applyParams(DocumentationBuilder docBuilder) {
            docBuilder.param(opModel.getInput().getVariableName(),
                             "a {@link Consumer} that will call methods on {@link %s.Builder}.",
                             opModel.getInputShape().getC2jName());
        }
    }
}
