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

import static software.amazon.awssdk.codegen.poet.eventstream.EventStreamUtils.isEventStreamParentModel;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.utils.StringUtils;

/**
 * MarshallerSpec for Json protocol
 */
public class JsonMarshallerSpec implements MarshallerProtocolSpec {

    protected final ShapeModel shapeModel;

    public JsonMarshallerSpec(ShapeModel shapeModel) {
        this.shapeModel = shapeModel;
    }

    @Override
    public ParameterSpec protocolFactoryParameter() {
        return ParameterSpec.builder(protocolFactoryClass(), "protocolFactory").build();
    }

    @Override
    public Optional<MethodSpec> constructor() {
        return Optional.of(MethodSpec.constructorBuilder()
                                     .addModifiers(Modifier.PUBLIC)
                                     .addParameter(protocolFactoryParameter())
                                     .addStatement("this.protocolFactory = protocolFactory")
                                     .build());
    }

    @Override
    public CodeBlock marshalCodeBlock(ClassName requestClassName) {
        String variableName = shapeModel.getVariable().getVariableName();
        return CodeBlock.builder()
                        .addStatement("$T<$T> protocolMarshaller = protocolFactory.createProtocolMarshaller"
                                      + "(SDK_OPERATION_BINDING)",
                                      ProtocolMarshaller.class, SdkHttpFullRequest.class)
                        .addStatement("return protocolMarshaller.marshall($L)", variableName)
                        .build();
    }

    @Override
    public FieldSpec protocolFactory() {
        return FieldSpec.builder(protocolFactoryClass(), "protocolFactory")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL).build();
    }

    private Class<BaseAwsJsonProtocolFactory> protocolFactoryClass() {
        return BaseAwsJsonProtocolFactory.class;
    }

    @Override
    public List<FieldSpec> memberVariables() {
        List<FieldSpec> fields = new ArrayList<>();
        fields.add(operationInfoField());
        fields.add(protocolFactory());
        return fields;
    }

    protected FieldSpec operationInfoField() {
        CodeBlock.Builder initializationCodeBlockBuilder = CodeBlock.builder()
                                                                    .add("$T.builder()", OperationInfo.class);
        initializationCodeBlockBuilder.add(".requestUri($S)", shapeModel.getMarshaller().getRequestUri())
                                      .add(".httpMethod($T.$L)", SdkHttpMethod.class, shapeModel.getMarshaller().getVerb())
                                      .add(".hasExplicitPayloadMember($L)", shapeModel.isHasPayloadMember() ||
                                                                            shapeModel.getExplicitEventPayloadMember() != null)
                                      .add(".hasPayloadMembers($L)", shapeModel.hasPayloadMembers());

        if (StringUtils.isNotBlank(shapeModel.getMarshaller().getTarget())) {
            initializationCodeBlockBuilder.add(".operationIdentifier($S)", shapeModel.getMarshaller().getTarget());
        }

        if (shapeModel.isHasStreamingMember()) {
            initializationCodeBlockBuilder.add(".hasStreamingInput(true)");
        }

        if (isEventStreamParentModel(shapeModel)) {
            initializationCodeBlockBuilder.add(".hasEventStreamingInput(true)");
        }

        CodeBlock codeBlock = initializationCodeBlockBuilder.add(".build()").build();

        return FieldSpec.builder(ClassName.get(OperationInfo.class), "SDK_OPERATION_BINDING")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                        .initializer(codeBlock)
                        .build();
    }
}
