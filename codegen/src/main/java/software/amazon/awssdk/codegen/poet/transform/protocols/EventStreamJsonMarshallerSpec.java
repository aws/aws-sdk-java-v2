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

package software.amazon.awssdk.codegen.poet.transform.protocols;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.eventstream.EventStreamUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;

/**
 * MarshallerSpec for event shapes in Json protocol
 */
public final class EventStreamJsonMarshallerSpec extends JsonMarshallerSpec {

    private static final String JSON_CONTENT_TYPE = "application/json";

    private final IntermediateModel intermediateModel;

    public EventStreamJsonMarshallerSpec(IntermediateModel model, ShapeModel shapeModel) {
        super(shapeModel);
        this.intermediateModel = model;
    }

    @Override
    public CodeBlock marshalCodeBlock(ClassName requestClassName) {
        String variableName = shapeModel.getVariable().getVariableName();
        CodeBlock.Builder builder =
            CodeBlock.builder()
                     .addStatement("$T<$T> protocolMarshaller = protocolFactory.createProtocolMarshaller(SDK_OPERATION_BINDING)",
                                   ProtocolMarshaller.class, SdkHttpFullRequest.class)
                     .add("return protocolMarshaller.marshall($L).toBuilder()", variableName)
                     .add(".putHeader(\":message-type\", \"event\")")
                     .add(".putHeader(\":event-type\", \"$L\")", getMemberNameFromEventStream());

        // Add :content-type header only if payload is present
        if (!shapeModel.hasNoEventPayload()) {
            builder.add(".putHeader(\":content-type\", \"$L\")", determinePayloadContentType());
        }

        builder.add(".build();");

        return builder.build();
    }

    @Override
    protected FieldSpec operationInfoField() {
        CodeBlock.Builder builder =
            CodeBlock.builder()
                     .add("$T.builder()", OperationInfo.class)
                     .add(".hasExplicitPayloadMember($L)", shapeModel.isHasPayloadMember() ||
                                                           shapeModel.getExplicitEventPayloadMember() != null)
                     .add(".hasPayloadMembers($L)", shapeModel.hasPayloadMembers())
                     // Adding httpMethod to avoid validation failure while creating the SdkHttpFullRequest
                     .add(".httpMethod($T.GET)", SdkHttpMethod.class)
                     .add(".hasEvent(true)")
                     .add(".build()");


        return FieldSpec.builder(ClassName.get(OperationInfo.class), "SDK_OPERATION_BINDING")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                        .initializer(builder.build())
                        .build();
    }

    private String getMemberNameFromEventStream() {
        ShapeModel eventStream = EventStreamUtils.getBaseEventStreamShape(intermediateModel, shapeModel)
            .orElseThrow(() -> new IllegalStateException("Could not find associated event stream spec for "
                                                         + shapeModel.getC2jName()));
        return eventStream.getMembers().stream()
                          .filter(memberModel -> memberModel.getShape().equals(shapeModel))
                          .findAny()
                          .map(MemberModel::getC2jName)
                          .orElseThrow(() -> new IllegalStateException(
                              String.format("Unable to find %s from its parent event stream", shapeModel.getC2jName())));
    }

    private String determinePayloadContentType() {
        MemberModel explicitEventPayload = shapeModel.getExplicitEventPayloadMember();
        if (explicitEventPayload != null) {
            return getPayloadContentType(explicitEventPayload);
        }

        return JSON_CONTENT_TYPE;
    }

    private String getPayloadContentType(MemberModel memberModel) {
        String blobContentType = "application/octet-stream";
        String stringContentType = "text/plain";
        String variableType = memberModel.getVariable().getVariableType();

        if ("software.amazon.awssdk.core.SdkBytes".equals(variableType)) {
            return blobContentType;
        } else if ("String".equals(variableType)) {
            return stringContentType;
        }

        return JSON_CONTENT_TYPE;
    }
}
