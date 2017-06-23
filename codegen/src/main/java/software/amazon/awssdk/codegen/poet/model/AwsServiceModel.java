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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;

/**
 * Provides the Poet specs for AWS Service models.
 */
public class AwsServiceModel implements ClassSpec {

    private final ShapeModel shapeModel;
    private final PoetExtensions poetExtensions;
    private final TypeProvider typeProvider;
    private final ShapeModelSpec shapeModelSpec;
    private final ShapeInterfaceProvider interfaceProvider;
    private final ModelMethodOverrides modelMethodOverrides;
    private final ModelBuilderSpecs modelBuilderSpecs;

    public AwsServiceModel(IntermediateModel intermediateModel, ShapeModel shapeModel) {
        this.shapeModel = shapeModel;
        this.poetExtensions = new PoetExtensions(intermediateModel);
        this.typeProvider = new TypeProvider(intermediateModel);
        this.shapeModelSpec = new ShapeModelSpec(this.shapeModel, typeProvider, poetExtensions);
        this.interfaceProvider = new AwsShapePublicInterfaceProvider(intermediateModel, this.shapeModel);
        this.modelMethodOverrides = new ModelMethodOverrides(this.poetExtensions);
        this.modelBuilderSpecs = new ModelBuilderSpecs(intermediateModel, this.shapeModel, this.shapeModelSpec,
                                                       this.typeProvider);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder specBuilder = TypeSpec.classBuilder(shapeModel.getShapeName())
                                               .addModifiers(Modifier.PUBLIC)
                                               .addAnnotation(PoetUtils.GENERATED)
                                               .addSuperinterfaces(modelSuperInterfaces())
                                               .superclass(modelSuperClass())
                                               .addMethods(modelClassMethods())
                                               .addFields(shapeModelSpec.fields())
                                               .addTypes(nestedModelClassTypes());

        if (shapeModel.getDocumentation() != null) {
            specBuilder.addJavadoc("$L", shapeModel.getDocumentation());
        }

        return specBuilder.build();
    }

    @Override
    public ClassName className() {
        return shapeModelSpec.className();
    }

    private List<TypeName> modelSuperInterfaces() {
        return interfaceProvider.interfacesToImplement().stream()
                                .sorted(Comparator.comparing(TypeName::toString))
                                .collect(Collectors.toList());
    }

    private TypeName modelSuperClass() {
        return interfaceProvider.baseClassToExtend();
    }

    private List<MethodSpec> modelClassMethods() {
        List<MethodSpec> methodSpecs = new ArrayList<>();

        switch (shapeModel.getShapeType()) {
            case Exception:
                methodSpecs.add(exceptionConstructor());
                methodSpecs.add(toBuilderMethod());
                methodSpecs.add(builderMethod());
                methodSpecs.add(serializableBuilderClass());
                methodSpecs.addAll(memberGetters());
                break;
            default:
                methodSpecs.addAll(memberGetters());
                methodSpecs.add(constructor());
                methodSpecs.add(toBuilderMethod());
                methodSpecs.add(builderMethod());
                methodSpecs.add(serializableBuilderClass());
                methodSpecs.add(modelMethodOverrides.hashCodeMethod(shapeModel));
                methodSpecs.add(modelMethodOverrides.equalsMethod(shapeModel));
                methodSpecs.add(modelMethodOverrides.toStringMethod(shapeModel));
                break;
        }

        if (implementStructuredPojoInterface()) {
            methodSpecs.add(structuredPojoMarshallMethod(shapeModel));
        }

        return methodSpecs;
    }

    private List<MethodSpec> memberGetters() {
        return shapeModel.getNonStreamingMembers().stream()
                         .filter(m -> !m.getHttp().getIsStreaming())
                         .map(m -> MethodSpec.methodBuilder(m.getFluentGetterMethodName())
                                             .addJavadoc("$L", m.getGetterDocumentation())
                                             .returns(typeProvider.fieldType(m))
                                             .addModifiers(Modifier.PUBLIC)
                                             .addStatement("return $N", m.getVariable().getVariableName())
                                             .build())
                         .collect(Collectors.toList());
    }

    private List<TypeSpec> nestedModelClassTypes() {
        List<TypeSpec> nestedClasses = new ArrayList<>();
        switch (shapeModel.getShapeType()) {
            case Model:
            case Request:
            case Response:
            case Exception:
                nestedClasses.add(modelBuilderSpecs.builderInterface());
                nestedClasses.add(modelBuilderSpecs.beanStyleBuilder());
                break;
            default:
                break;
        }
        return nestedClasses;
    }

    private boolean implementStructuredPojoInterface() {
        return interfaceProvider.shouldImplementInterface(StructuredPojo.class);
    }

    private MethodSpec structuredPojoMarshallMethod(ShapeModel shapeModel) {
        return MethodSpec.methodBuilder("marshall")
                         .addAnnotation(SdkInternalApi.class)
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(ProtocolMarshaller.class, "protocolMarshaller")
                         .addStatement("$T.getInstance().marshall(this, $N)",
                                       poetExtensions.getTransformClass(shapeModel.getShapeName() + "Marshaller"),
                                       "protocolMarshaller")
                         .build();
    }

    private MethodSpec constructor() {
        MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder()
                                                   .addModifiers(Modifier.PRIVATE)
                                                   .addParameter(modelBuilderSpecs.builderImplName(), "builder");

        shapeModelSpec.fields().forEach(f -> ctorBuilder.addStatement("this.$N = builder.$N", f, f));

        return ctorBuilder.build();
    }

    private MethodSpec exceptionConstructor() {
        MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder()
                                                   .addModifiers(Modifier.PRIVATE)
                                                   .addParameter(modelBuilderSpecs.builderImplName(), "builder");

        ctorBuilder.addStatement("super(builder.message)");

        shapeModelSpec.fields().forEach(f -> ctorBuilder.addStatement("this.$N = builder.$N", f, f));

        return ctorBuilder.build();
    }

    private MethodSpec builderMethod() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .returns(modelBuilderSpecs.builderInterfaceName())
                         .addStatement("return new $T()", modelBuilderSpecs.builderImplName())
                         .build();
    }

    private MethodSpec toBuilderMethod() {
        return MethodSpec.methodBuilder("toBuilder")
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class)
                         .returns(modelBuilderSpecs.builderInterfaceName())
                         .addStatement("return new $T(this)", modelBuilderSpecs.builderImplName())
                         .build();
    }

    private MethodSpec serializableBuilderClass() {
        return MethodSpec.methodBuilder("serializableBuilderClass")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .returns(ParameterizedTypeName.get(ClassName.get(Class.class),
                                                            WildcardTypeName.subtypeOf(modelBuilderSpecs.builderInterfaceName())))
                         .addStatement("return $T.class", modelBuilderSpecs.builderImplName())
                         .build();
    }
}
