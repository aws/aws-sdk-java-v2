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
import software.amazon.awssdk.codegen.poet.transform.protocols.EventStreamJsonMarshallerSpec;
import software.amazon.awssdk.codegen.poet.transform.protocols.JsonMarshallerSpec;
import software.amazon.awssdk.codegen.poet.transform.protocols.MarshallerProtocolSpec;
import software.amazon.awssdk.codegen.poet.transform.protocols.QueryMarshallerSpec;
import software.amazon.awssdk.codegen.poet.transform.protocols.XmlMarshallerSpec;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.Validate;

public class MarshallerSpec implements ClassSpec {

    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final ClassName baseMashallerName;
    private final TypeName httpRequestName;
    private final ClassName requestName;
    private final ClassName className;
    private final ClassName requestClassName;
    private final MarshallerProtocolSpec protocolSpec;

    public MarshallerSpec(IntermediateModel intermediateModel, ShapeModel shapeModel) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        String modelPackage = intermediateModel.getMetadata().getFullModelPackageName();
        this.baseMashallerName = ClassName.get(Marshaller.class);
        this.httpRequestName = ClassName.get(SdkHttpFullRequest.class);
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
                       .addAnnotation(PoetUtils.generatedAnnotation())
                       .addAnnotation(SdkInternalApi.class)
                       .addSuperinterface(ParameterizedTypeName.get(baseMashallerName, requestName))
                       .addFields(protocolSpec.memberVariables())
                       .addFields(protocolSpec.additionalFields())
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
                                                         .returns(httpRequestName);

        methodSpecBuilder.addStatement("$T.paramNotNull($L, $S)", ClassName.get(Validate.class), variableName, variableName);
        methodSpecBuilder.beginControlFlow("try");

        methodSpecBuilder.addCode(protocolSpec.marshalCodeBlock(requestClassName));

        methodSpecBuilder.endControlFlow();
        methodSpecBuilder.beginControlFlow("catch (Exception e)");
        methodSpecBuilder.addStatement("throw $T.builder().message(\"Unable to marshall request to JSON: \" + " +
                "e.getMessage()).cause(e).build()", ClassName
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
                return getJsonMarshallerSpec();

            case QUERY:
            case EC2:
                return new QueryMarshallerSpec(intermediateModel, shapeModel);

            case REST_XML:
                return new XmlMarshallerSpec(intermediateModel, shapeModel);

            case API_GATEWAY:
                throw new UnsupportedOperationException("Not yet supported.");
            default:
                throw new RuntimeException("Unknown protocol: " + protocol.name());
        }
    }

    private MarshallerProtocolSpec getJsonMarshallerSpec() {
        if (shapeModel.isEvent()) {
            return new EventStreamJsonMarshallerSpec(intermediateModel, shapeModel);
        }
        return new JsonMarshallerSpec(shapeModel);
    }
}
