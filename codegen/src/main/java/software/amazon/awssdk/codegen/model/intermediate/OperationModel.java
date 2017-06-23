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

package software.amazon.awssdk.codegen.model.intermediate;

import static software.amazon.awssdk.codegen.internal.Constants.LF;
import static software.amazon.awssdk.codegen.internal.DocumentationUtils.createLinkToServiceDocumentation;
import static software.amazon.awssdk.codegen.internal.DocumentationUtils.stripHtmlTags;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.codegen.internal.DocumentationUtils;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.utils.CollectionUtils;

public class OperationModel extends DocumentationModel {

    private String operationName;

    private boolean deprecated;

    private VariableModel input;

    private String inputStreamPropertyName;

    private ReturnTypeModel returnType;

    private List<ExceptionModel> exceptions = new ArrayList<ExceptionModel>();

    private List<SimpleMethodFormModel> simpleMethods;

    private boolean hasBlobMemberAsPayload;

    private boolean isAuthenticated = true;

    @JsonIgnore
    private ShapeModel inputShape;

    @JsonIgnore
    private ShapeModel outputShape;

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public String getMethodName() {
        return Utils.unCapitialize(operationName);
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getSyncDocumentation(IntermediateModel model, OperationModel opModel) {
        return getDocumentation(MethodType.SYNC, model, opModel);
    }

    public String getAsyncDocumentation(IntermediateModel model, OperationModel opModel) {
        return getDocumentation(MethodType.ASYNC, model, opModel);
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setIsAuthenticated(boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
    }

    public ShapeModel getInputShape() {
        return inputShape;
    }

    public void setInputShape(ShapeModel inputShape) {
        this.inputShape = inputShape;
    }

    public ShapeModel getOutputShape() {
        return outputShape;
    }

    public void setOutputShape(ShapeModel outputShape) {
        this.outputShape = outputShape;
    }

    private static enum MethodType {

        SYNC(false),
        ASYNC(true),
        ASYNC_WITH_HANDLER(true);

        private final boolean async;

        private MethodType(boolean async) {
            this.async = async;
        }

        public boolean isAsync() {
            return async;
        }
    }

    private String getDocumentation(final MethodType methodType, IntermediateModel model, OperationModel opModel) {
        Metadata md = model.getMetadata();
        StringBuilder docBuilder = new StringBuilder();

        if (documentation != null) {
            docBuilder.append(documentation);
        } else {
            docBuilder.append("Invokes the ").append(operationName).append(" operation");

            if (methodType.isAsync()) {
                docBuilder.append(" asynchronously");
            }

            docBuilder.append(".");
        }

        if (input != null) {
            docBuilder.append(LF).append("@param ").append(input.getVariableName())
                      .append(" ").append(stripHtmlTags(input.getDocumentation()));
        }


        if (methodType == MethodType.ASYNC) {
            if (opModel.hasStreamingInput()) {
                String streamMemberDocs = getStreamingMemberDocs(opModel.getInputShape());
                docBuilder.append(LF)
                          .append("@param requestProvider ")
                          .append("Functional interface that can be implemented to produce the request content ")
                          .append("in a non-blocking manner. The size of the content is expected to be known up front. ")
                          .append("See {@link AsyncRequestProvider} for specific details on implementing this interface as well ")
                          .append("as links to precanned implementations for common scenarios like uploading from a file. The ")
                          .append("service documentation for the request content is as follows '")
                          .append(streamMemberDocs)
                          .append("'.");
            }
            if (opModel.hasStreamingOutput()) {
                String streamMemberDocs = getStreamingMemberDocs(opModel.getOutputShape());
                docBuilder.append(LF)
                          .append("@param asyncResponseHandler The response handler for processing the streaming response in a ")
                          .append("non-blocking manner. See {@link AsyncResponseHandler} for details on how this callback ")
                          .append("should be implemented and for links to precanned implementations for common scenarios like ")
                          .append("downloading to a file. The service documentation for the streamed content is as follows '")
                          .append(streamMemberDocs).append("'.");
            }
        } else if (methodType == MethodType.SYNC) {
            if (opModel.hasStreamingInput()) {
                String streamMemberDocs = getStreamingMemberDocs(opModel.getInputShape());
                docBuilder.append(LF)
                          .append("@param requestBody ")
                          .append("The content to send to the service. A {@link RequestBody} can be created using one of ")
                          .append("several factory methods for various sources of data. For example, to create a request body ")
                          .append("from a file you can do the following. ")
                          .append("<pre>{@code RequestBody.of(new File(\"myfile.txt\"))}</pre>")
                          .append("See documentation in {@link RequestBody} for additional details and which sources of data ")
                          .append("are supported. The service documentation for the request content is as follows '")
                          .append(streamMemberDocs).append("'.");
            }
            if (opModel.hasStreamingOutput()) {
                String streamMemberDocs = getStreamingMemberDocs(opModel.getOutputShape());
                docBuilder.append(LF)
                          .append("@param streamingHandler ")
                          .append("Functional interface for processing the streamed response content. The unmarshalled ")
                          .append(opModel.getOutputShape().getShapeName())
                          .append(" and an InputStream to the response content are provided as parameters to the callback. ")
                          .append("The callback may return a transformed type which will be the return value of this method. ")
                          .append("See {@link software.amazon.awssdk.runtime.transform.StreamingResponseHandler} for details on ")
                          .append("implementing this interface and for links to precanned implementations for common scenarios ")
                          .append("like downloading to a file. The service documentation for the response content is as ")
                          .append("follows '").append(streamMemberDocs).append("'.");
            }
        }

        if (returnType != null) {
            docBuilder.append(LF).append("@return ");
            if (methodType.isAsync()) {
                if (opModel.hasStreamingOutput()) {
                    docBuilder.append("A future to the transformed result of the AsyncResponseHandler.");
                } else {
                    docBuilder.append(DocumentationUtils.DEFAULT_ASYNC_RETURN.replace("%s", operationName));
                }
            } else {
                if (opModel.hasStreamingOutput()) {
                    docBuilder.append("The transformed result of the StreamingResponseHandler.");
                } else {
                    docBuilder.append(DocumentationUtils.DEFAULT_SYNC_RETURN.replace("%s", operationName));
                }
            }
        }

        if (!methodType.isAsync() && !CollectionUtils.isNullOrEmpty(exceptions)) {
            for (ExceptionModel exception : exceptions) {
                docBuilder.append(LF).append("@throws ")
                          .append(exception.getExceptionName()).append(" ")
                          .append(stripHtmlTags(exception.getDocumentation()));
            }
            docBuilder.append(LF)
                      .append("@throws SdkBaseException Base class for all exceptions that can be thrown ")
                      .append("by the SDK (both service and client). Can be used for catch all scenarios.")
                      .append(LF)
                      .append("@throws SdkClientException If any client side error occurs such as an IO related ")
                      .append("failure, failure to get credentials, etc)")
                      .append(LF)
                      .append("@throws ").append(md.getBaseExceptionName())
                      .append(" Base exception for all service exceptions. Unknown exceptions will be thrown as an ")
                      .append("instance of this type")
            ;
        } else if (methodType.isAsync() && !CollectionUtils.isNullOrEmpty(exceptions)) {
            docBuilder.append(LF).append("<br/>The CompletableFuture returned by this method can be " +
                                         "completed exceptionally with the following exceptions.")
                      .append("<ul>");
            exceptions.forEach(e -> docBuilder.append("\n<li>")
                                              .append(e.getExceptionName()).append(" ")
                                              .append(stripHtmlTags(e.getDocumentation()))
                                              .append("</li>"));
            docBuilder
                    .append("\n<li>SdkBaseException Base class for all exceptions that can be thrown ")
                    .append("by the SDK (both service and client). Can be used for catch all scenarios.</li>")
                    .append("\n<li>SdkClientException If any client side error occurs ")
                    .append("such as an IO related failure, failure to get credentials, etc</li>")
                    .append("\n<li>")
                    .append(md.getBaseExceptionName()).append(" Base class for all service exceptions. ")
                    .append("Unknown exceptions will be thrown as an instance of this </li > ")
                    .append("<ul>");
        }

        docBuilder.append(getSampleTagForMethodType(methodType, md));
        String crosslink = createLinkToServiceDocumentation(md, operationName);
        if (!crosslink.isEmpty()) {
            docBuilder.append(LF).append(crosslink);
        }

        return docBuilder.toString().replace("$", "&#36;");
    }

    /**
     * @param streamingShape Shape containing streaming member.
     * @return Documentation for the streaming member in the given Shape.
     * @throws IllegalStateException if shape does not have streaming member.
     */
    private String getStreamingMemberDocs(ShapeModel streamingShape) {
        return streamingShape.getMembers().stream()
                             .filter(m -> m.getHttp().getIsStreaming())
                             .map(DocumentationModel::getDocumentation)
                             .findFirst()
                             .orElseThrow(() -> new IllegalStateException(
                                     "Streaming member not found in " + streamingShape.getShapeName()));
    }

    private String getSampleTagForMethodType(final MethodType methodType, final Metadata md) {
        StringBuilder sb = new StringBuilder();

        sb.append(LF).append("@sample ");

        if (methodType == MethodType.SYNC) {
            sb.append(md.getSyncInterface());
        } else if (methodType == MethodType.ASYNC) {
            sb.append(md.getAsyncInterface());
        } else if (methodType == MethodType.ASYNC_WITH_HANDLER) {
            sb.append(md.getAsyncInterface() + "Handler");
        }
        sb.append(".").append(operationName);
        return sb.toString();
    }

    public VariableModel getInput() {
        return input;
    }

    public void setInput(VariableModel input) {
        this.input = input;
    }

    public String getInputStreamPropertyName() {
        return inputStreamPropertyName;
    }

    public void setInputStreamPropertyName(String inputStreamPropertyName) {
        this.inputStreamPropertyName = inputStreamPropertyName;
    }

    public ReturnTypeModel getReturnType() {
        return returnType;
    }

    public void setReturnType(ReturnTypeModel returnType) {
        this.returnType = returnType;
    }

    private String getBaseReturnType(boolean async) {
        if (returnType == null) {
            if (async) {
                return "Void";
            } else {
                return "void";
            }
        }
        return returnType.getReturnType();
    }

    public String getSyncReturnType() {
        return getBaseReturnType(false);
    }

    public String getAsyncReturnType() {
        return getBaseReturnType(true);
    }

    public String getAsyncFutureType() {
        return "CompletableFuture<" + getAsyncReturnType() + ">";
    }

    public String getAsyncCallableType() {
        return "java.util.concurrent.Callable<" + getAsyncReturnType() + ">";
    }

    public String getAsyncHandlerType() {
        return "software.amazon.awssdk.handlers.AsyncHandler<" + input.getVariableType() + ", " +
               getAsyncReturnType() + ">";
    }

    public List<ExceptionModel> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<ExceptionModel> exceptions) {
        this.exceptions = exceptions;
    }

    public void addException(ExceptionModel exception) {
        exceptions.add(exception);
    }

    @JsonIgnore
    public List<SimpleMethodFormModel> getSimpleMethodForms() {
        return simpleMethods;
    }

    public void addSimpleMethodForm(List<ArgumentModel> arguments) {
        if (this.simpleMethods == null) {
            this.simpleMethods = new ArrayList<>();
        }

        SimpleMethodFormModel form = new SimpleMethodFormModel();
        form.setArguments(arguments);

        this.simpleMethods.add(form);
    }

    public boolean getHasBlobMemberAsPayload() {
        return this.hasBlobMemberAsPayload;
    }

    public void setHasBlobMemberAsPayload(boolean hasBlobMemberAsPayload) {
        this.hasBlobMemberAsPayload = hasBlobMemberAsPayload;
    }

    public boolean hasStreamingInput() {
        return inputShape != null && inputShape.isHasStreamingMember();
    }

    public boolean hasStreamingOutput() {
        return outputShape != null && outputShape.isHasStreamingMember();
    }
}
