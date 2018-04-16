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

import static software.amazon.awssdk.codegen.internal.DocumentationUtils.createLinkToServiceDocumentation;
import static software.amazon.awssdk.codegen.internal.DocumentationUtils.stripHtmlTags;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.model.intermediate.DocumentationModel;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Base class for providers of documentation for operation methods.
 */
abstract class OperationDocProvider {

    /**
     * Doc string for {@link java.nio.file.Path} parameter in simple method overload for streaming input operations.
     */
    static final String SIMPLE_FILE_INPUT_DOCS =
            "{@link Path} to file containing data to send to the service. File will be read entirely and " +
            "may be read multiple times in the event of a retry. If the file does not exist or the " +
            "current user does not have access to read it then an exception will be thrown. ";

    /**
     * Doc string for {@link java.nio.file.Path} parameter in simple method overload for streaming output operations.
     */
    static final String SIMPLE_FILE_OUTPUT_DOCS =
            "{@link Path} to file that response contents will be written to. The file must not exist or " +
            "this method will throw an exception. If the file is not writable by the current user then " +
            "an exception will be thrown. ";

    protected final IntermediateModel model;
    protected final OperationModel opModel;
    protected final DocConfiguration config;
    protected final PaginationDocs paginationDocs;

    OperationDocProvider(IntermediateModel model, OperationModel opModel, DocConfiguration config) {
        this.model = model;
        this.opModel = opModel;
        this.config = config;
        this.paginationDocs = new PaginationDocs(model, opModel);
    }

    /**
     * @return Constructs the Javadoc for this operation method overload.
     */
    String getDocs() {
        DocumentationBuilder docBuilder = new DocumentationBuilder();

        String description = StringUtils.isNotBlank(opModel.getDocumentation()) ?
                             opModel.getDocumentation() :
                             getDefaultServiceDocs();

        String appendedDescription = appendToDescription();

        if (config.isConsumerBuilder()) {
            appendedDescription += getConsumerBuilderDocs();
        }

        docBuilder.description(StringUtils.isNotBlank(appendedDescription) ?
                               description + "<br/>" + appendedDescription :
                               description);

        applyParams(docBuilder);
        applyReturns(docBuilder);
        applyThrows(docBuilder);
        docBuilder.tag("sample", getInterfaceName() + "." + opModel.getOperationName());

        String crosslink = createLinkToServiceDocumentation(model.getMetadata(), opModel.getOperationName());
        if (!crosslink.isEmpty()) {
            docBuilder.see(crosslink);
        }
        return docBuilder.build().replace("$", "&#36");
    }

    /**
     * @return A string that will be appended to the standard description.
     */
    protected String appendToDescription() {
        return "";
    }

    /**
     * @return Documentation describing the streaming input parameter. Uses documentation for the streaming member in the model.
     */
    final String getStreamingInputDocs() {
        return String.format("The service documentation for the request content is as follows '%s'",
                             getStreamingMemberDocs(opModel.getInputShape()));
    }

    /**
     * @return Documentation describing the streaming output parameter. Uses documentation for the streaming member in the model.
     */
    final String getStreamingOutputDocs() {
        return String.format("The service documentation for the response content is as follows '%s'.",
                             getStreamingMemberDocs(opModel.getOutputShape()));
    }

    /**
     * @return Documentation describing the consumer-builder variant of a method.
     */
    private String getConsumerBuilderDocs() {
        return "<p>This is a convenience which creates an instance of the {@link " +
               opModel.getInput().getSimpleType() +
               ".Builder} avoiding the need to create one manually via {@link " +
               opModel.getInput().getSimpleType() +
               "#builder()}</p>";
    }

    /**
     * Gets the member documentation (as defined in the C2J model) for the streaming member of a shape.
     *
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

    /**
     * @return List of thrown exceptions for the given operation. Includes both modeled exceptions and SDK base exceptions.
     */
    final List<Pair<String, String>> getThrows() {
        List<Pair<String, String>> throwsDocs = opModel.getExceptions().stream()
                                                       .map(exception -> Pair.of(exception.getExceptionName(),
                                                                                    stripHtmlTags(exception.getDocumentation())))
                                                       .collect(Collectors.toList());
        String baseServiceException = model.getMetadata().getBaseExceptionName();
        Collections.addAll(throwsDocs,
                           Pair.of("SdkException ", "Base class for all exceptions that can be thrown by the SDK " +
                                                           "(both service and client). Can be used for catch all scenarios."),
                           Pair.of("SdkClientException ", "If any client side error occurs such as an IO related failure, " +
                                                             "failure to get credentials, etc."),
                           Pair.of(baseServiceException, "Base class for all service exceptions. Unknown exceptions will be " +
                                                            "thrown as an instance of this type."));
        return throwsDocs;
    }

    /**
     * Emits documentation for the request object to the {@link DocumentationBuilder}.
     *
     * @param docBuilder {@link DocumentationBuilder} to emit param to.
     */
    final void emitRequestParm(DocumentationBuilder docBuilder) {
        String parameterDocs = stripHtmlTags(opModel.getInput().getDocumentation());

        if (config.isConsumerBuilder()) {
            docBuilder.param(opModel.getInput().getVariableName(),
                             "A {@link Consumer} that will call methods on {@link %s.Builder} to create a request. %s",
                             opModel.getInputShape().getC2jName(),
                             parameterDocs);
        } else {
            docBuilder.param(opModel.getInput().getVariableName(), parameterDocs);
        }
    }

    /**
     * @return The interface name of the client. Will differ per {@link ClientType}.
     */
    protected abstract String getInterfaceName();

    /**
     * @return Default documentation to put in {@link DocumentationBuilder#description(String)} when no service docs are present.
     * Will differ per {@link ClientType}.
     */
    protected abstract String getDefaultServiceDocs();

    /**
     * Add any relevant params to the {@link DocumentationBuilder}. Depends on {@link ClientType} and which overload we are
     * generating documentation for.
     *
     * @param docBuilder {@link DocumentationBuilder} to add params to.
     */
    protected abstract void applyParams(DocumentationBuilder docBuilder);

    /**
     * Add documentation describing the return value (if any).
     *
     * @param docBuilder {@link DocumentationBuilder} to add return documentation for.
     */
    protected abstract void applyReturns(DocumentationBuilder docBuilder);

    /**
     * Adds documentation describing the thrown exceptions (if any).
     *
     * @param docBuilder {@link DocumentationBuilder} to add throws documentation for.
     */
    protected abstract void applyThrows(DocumentationBuilder docBuilder);

}
