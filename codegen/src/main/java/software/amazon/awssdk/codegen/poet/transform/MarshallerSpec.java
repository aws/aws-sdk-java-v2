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

package software.amazon.awssdk.codegen.poet.transform;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.transform.protocols.JsonMarshallerSpec;
import software.amazon.awssdk.codegen.poet.transform.protocols.MarshallerProtocolSpec;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.utils.Validate;

public class MarshallerSpec implements ClassSpec {

    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final ClassName baseMashallerName;
    private final TypeName requestWrapperName;
    private final ClassName requestName;
    private final ClassName className;
    private final ClassName requestClassName;
    private final MarshallerProtocolSpec protocolSpec;

    public MarshallerSpec(IntermediateModel intermediateModel, ShapeModel shapeModel) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        String modelPackage = intermediateModel.getMetadata().getFullModelPackageName();
        this.baseMashallerName = ClassName.get(Marshaller.class);
        ClassName modelRequestClass = ClassName.get(modelPackage, shapeModel.getShapeName());
        this.requestWrapperName = ParameterizedTypeName.get(ClassName.get(Request.class), modelRequestClass);
        this.requestName = ClassName.get(modelPackage, shapeModel.getShapeName());
        this.className = new PoetExtensions(intermediateModel).getRequestTransformClass(shapeModel.getShapeName() + "Marshaller");
        this.requestClassName = ClassName.get(modelPackage, shapeModel.getShapeName());
        this.protocolSpec = getProtocolSpecs(intermediateModel.getMetadata().getProtocol());
    }

    @Override
    public TypeSpec poetSpec() {
        return TypeSpec.classBuilder(className)
                       .addJavadoc("{@link $T} Marshaller", requestClassName)
                       .addModifiers(Modifier.PUBLIC)
                       .addAnnotation(PoetUtils.GENERATED)
                       .addAnnotation(SdkInternalApi.class)
                       .addSuperinterface(
                           ParameterizedTypeName.get(baseMashallerName,
                                                     requestWrapperName,
                                                     requestName))
                       .addFields(protocolSpec.memberVariables())
                       .addMethods(methods())
                       .build();
    }


    @Override
    public ClassName className() {
        return className;
    }

    private List<MethodSpec> methods() {
        List<MethodSpec> methodSpecs = new ArrayList<>();

        protocolSpec.constructor().ifPresent(methodSpecs::add);
        methodSpecs.add(marshallMethod());
        methodSpecs.addAll(protocolSpec.additionalMethods());
        return methodSpecs;
    }

    private MethodSpec marshallMethod() {
        String variableName = shapeModel.getVariable().getVariableName();
        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("marshall")
                                                         .addAnnotation(Override.class)
                                                         .addModifiers(Modifier.PUBLIC)
                                                         .addParameter(requestClassName, variableName)
                                                         .returns(requestWrapperName);

        methodSpecBuilder.addStatement("$T.paramNotNull($L, $S)", ClassName.get(Validate.class), variableName, variableName);
        methodSpecBuilder.beginControlFlow("try");

        methodSpecBuilder.addCode(protocolSpec.marshalCodeBlock(requestClassName));

        methodSpecBuilder.endControlFlow();
        methodSpecBuilder.beginControlFlow("catch (Exception e)");
        methodSpecBuilder.addStatement("throw new $T(\"Unable to marshall request to JSON: \" + e.getMessage(), e)", ClassName
            .get(SdkClientException.class));
        methodSpecBuilder.endControlFlow();
        return methodSpecBuilder.build();
    }

    private MarshallerProtocolSpec getProtocolSpecs(software.amazon.awssdk.codegen.model.intermediate.Protocol protocol) {
        switch (protocol) {
            case REST_JSON:
            case CBOR:
            case ION:
            case AWS_JSON:
                return new JsonMarshallerSpec(intermediateModel, shapeModel);
            case QUERY:
            case REST_XML:
            case EC2:
            case API_GATEWAY:
                throw new UnsupportedOperationException("Not yet supported.");
            default:
                throw new RuntimeException("Unknown protocol: " + protocol.name());
        }
    }
}
