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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

public class EventModelBuilderSpecs {
    private final ClassName eventClassName;
    private final ShapeModelSpec shapeModelSpec;

    public EventModelBuilderSpecs(IntermediateModel intermediateModel, MemberModel eventModel, ClassName eventClassName,
                                  TypeProvider typeProvider) {
        this.eventClassName = eventClassName;
        this.shapeModelSpec = new ShapeModelSpec(eventModel.getShape(), typeProvider,
                new PoetExtensions(intermediateModel), intermediateModel);
    }

    public ClassName builderInterfaceName() {
        return classToBuild().nestedClass("Builder");
    }

    public ClassName builderImplName() {
        return classToBuild().nestedClass("BuilderImpl");
    }

    public TypeSpec builderInterface() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(builderInterfaceName())
                .addSuperinterfaces(superInterfaces())
                .addModifiers(Modifier.PUBLIC);


        builder.addMethod(MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(Override.class)
                .returns(eventClassName)
                .build());
        return builder.build();
    }

    public TypeSpec beanStyleBuilder() {
        return TypeSpec.classBuilder(builderImplName())
                .addSuperinterfaces(Collections.singletonList(builderInterfaceName()))
                .superclass(superClass())
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addMethod(noArgCConstructor())
                .addMethod(fromEventConstructor())
                .addMethod(buildMethod())
                .build();
    }

    private ClassName classToBuild() {
        return eventClassName;
    }

    private ClassName baseEventClassName() {
        return shapeModelSpec.className();
    }

    private List<TypeName> superInterfaces() {
        return Collections.singletonList(baseEventClassName().nestedClass("Builder"));
    }

    private TypeName superClass() {
        return baseEventClassName().nestedClass("BuilderImpl");
    }

    private MethodSpec buildMethod() {
        return MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addStatement("return new $T(this)", classToBuild())
                .returns(classToBuild())
                .build();
    }

    private MethodSpec noArgCConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build();
    }

    private MethodSpec fromEventConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(classToBuild(), "event")
                .addStatement("super(event)")
                .build();
    }
}
