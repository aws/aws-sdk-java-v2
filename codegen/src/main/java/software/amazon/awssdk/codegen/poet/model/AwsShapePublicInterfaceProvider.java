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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.AmazonWebServiceResult;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

public class AwsShapePublicInterfaceProvider implements ShapeInterfaceProvider {
    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final PoetExtensions poetExtensions;

    public AwsShapePublicInterfaceProvider(IntermediateModel intermediateModel, ShapeModel shapeModel) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        this.poetExtensions = new PoetExtensions(intermediateModel);
    }

    @Override
    public boolean shouldImplementInterface(Class<?> iface) {
        return interfacesToImplement().contains(ClassName.get(iface));
    }

    @Override
    public Set<TypeName> interfacesToImplement() {
        Set<TypeName> superInterfaces = new HashSet<>();

        switch (shapeModel.getShapeType()) {
            case Exception:
            case Request:
            case Model:
            case Response:
                superInterfaces.add(toCopyableBuilderInterface());
                break;
            default:
                break;
        }

        if (implementStructuredPojoInterface()) {
            superInterfaces.add(ClassName.get(StructuredPojo.class));
        }

        return superInterfaces;
    }

    @Override
    public TypeName baseClassToExtend() {
        switch (shapeModel.getShapeType()) {
            case Request:
                return ClassName.get(AmazonWebServiceRequest.class);
            case Response:
                if (intermediateModel.getMetadata().getProtocol() == Protocol.API_GATEWAY) {
                    return ClassName.get("software.amazon.awssdk.opensdk", "BaseResult");
                }
                return ParameterizedTypeName.get(AmazonWebServiceResult.class, ResponseMetadata.class);
            case Exception:
                return exceptionBaseClass();
            case Model:
            default:
                return ClassName.OBJECT;
        }
    }

    private TypeName exceptionBaseClass() {
        final String customExceptionBase = intermediateModel.getCustomizationConfig()
                .getSdkModeledExceptionBaseClassName();
        if (customExceptionBase != null) {
            return poetExtensions.getModelClass(customExceptionBase);
        }
        return poetExtensions.getModelClass(intermediateModel.getSdkModeledExceptionBaseClassName());

    }

    private boolean implementStructuredPojoInterface() {
        return intermediateModel.getMetadata().isJsonProtocol() && shapeModel.getShapeType() == ShapeType.Model;
    }

    private TypeName toCopyableBuilderInterface() {
        return ParameterizedTypeName.get(ClassName.get(ToCopyableBuilder.class),
                modelClassName().nestedClass("Builder"),
                modelClassName());
    }

    private ClassName modelClassName() {
        return poetExtensions.getModelClass(shapeModel.getShapeName());
    }
}
