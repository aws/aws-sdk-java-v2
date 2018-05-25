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

package software.amazon.awssdk.codegen.poet.transform.protocols;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.core.http.HttpMethodName;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.ProtocolRequestMarshaller;
import software.amazon.awssdk.utils.StringUtils;

/**
 * MarshallerSpec for Json protocol
 */
public class JsonMarshallerSpec implements MarshallerProtocolSpec {

    private final Metadata metadata;
    private final ShapeModel shapeModel;
    private final PoetExtensions poetExtensions;

    public JsonMarshallerSpec(IntermediateModel model, ShapeModel shapeMode) {
        this.metadata = model.getMetadata();
        this.poetExtensions = new PoetExtensions(model);
        this.shapeModel = shapeMode;
    }

    @Override
    public ParameterSpec protocolFactoryParameter() {
        return ParameterSpec.builder(AwsJsonProtocolFactory.class, "protocolFactory").build();
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
                                      + "(SDK_OPERATION_BINDING, $L)",
                                      ProtocolRequestMarshaller.class,
                                      requestClassName, variableName)
                        .addStatement("protocolMarshaller.startMarshalling()")
                        .addStatement("$T.getInstance().marshall($L, protocolMarshaller)",
                                      poetExtensions.getTransformClass(shapeModel.getShapeName() + "ModelMarshaller"),
                                      variableName)
                        .addStatement("return protocolMarshaller.finishMarshalling()")
                        .build();
    }

    @Override
    public FieldSpec protocolFactory() {
        return FieldSpec.builder(AwsJsonProtocolFactory.class, "protocolFactory")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL).build();
    }

    @Override
    public List<FieldSpec> memberVariables() {
        List<FieldSpec> fields = new ArrayList<>();

        CodeBlock.Builder initializationCodeBlockBuilder = CodeBlock.builder()
                                                                    .add("$T.builder()", OperationInfo.class);
        initializationCodeBlockBuilder.add(".requestUri($S)", shapeModel.getMarshaller().getRequestUri())
                                      .add(".httpMethodName($T.$L)", HttpMethodName.class, shapeModel.getMarshaller().getVerb())
                                      .add(".hasExplicitPayloadMember($L)", shapeModel.isHasPayloadMember())
                                      .add(".hasPayloadMembers($L)", shapeModel.hasPayloadMembers());

        if (StringUtils.isNotBlank(shapeModel.getMarshaller().getTarget())) {
            initializationCodeBlockBuilder.add(".operationIdentifier($S)", shapeModel.getMarshaller().getTarget())
                                          .add(".serviceName($S)", metadata.getServiceName());
        }

        CodeBlock codeBlock = initializationCodeBlockBuilder.add(".build()").build();

        FieldSpec instance = FieldSpec.builder(ClassName.get(OperationInfo.class), "SDK_OPERATION_BINDING")
                                      .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                                      .initializer(codeBlock)
                                      .build();

        FieldSpec protocolFactory = protocolFactory();

        fields.add(instance);
        fields.add(protocolFactory);
        return fields;
    }

}
