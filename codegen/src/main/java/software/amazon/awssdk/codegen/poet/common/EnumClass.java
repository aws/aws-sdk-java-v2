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

package software.amazon.awssdk.codegen.poet.common;

import static software.amazon.awssdk.codegen.poet.PoetUtils.addDeprecated;
import static software.amazon.awssdk.codegen.poet.PoetUtils.addJavadoc;
import static software.amazon.awssdk.codegen.poet.PoetUtils.createEnumBuilder;
import static software.amazon.awssdk.codegen.poet.PoetUtils.toStringBuilder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.StaticImport;

public final class EnumClass implements ClassSpec {

    private static final String VALUE = "value";
    private static final String UNKNOWN_TO_SDK_VERSION = "UNKNOWN_TO_SDK_VERSION";
    private final ShapeModel shape;
    private final ClassName className;

    public EnumClass(String enumPackage, ShapeModel shape) {
        this.shape = shape;
        this.className = ClassName.get(enumPackage, shape.getShapeName());
    }

    @Override
    public TypeSpec poetSpec() {
        Builder enumBuilder = createEnumBuilder(className)
            .addField(String.class, VALUE, Modifier.PRIVATE, Modifier.FINAL)
            .addMethod(toStringBuilder().addStatement("return $T.valueOf($N)", String.class, VALUE).build())
            .addMethod(fromValueSpec())
            .addMethod(knownValuesSpec())
            .addMethod(createConstructor());

        addDeprecated(enumBuilder::addAnnotation, shape);
        addJavadoc(enumBuilder::addJavadoc, shape);

        shape.getEnums().forEach(
            e -> enumBuilder.addEnumConstant(e.getName(), TypeSpec.anonymousClassBuilder("$S", e.getValue()).build())
        );
        enumBuilder.addEnumConstant(UNKNOWN_TO_SDK_VERSION, TypeSpec.anonymousClassBuilder("null").build());

        return enumBuilder.build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    @Override
    public Iterable<StaticImport> staticImports() {
        return Collections.singleton(StaticImport.staticMethodImport(Collectors.class, "toSet"));
    }

    private MethodSpec createConstructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PRIVATE)
                         .addParameter(String.class, VALUE)
                         .addStatement("this.$1N = $1N", VALUE)
                         .build();
    }

    private MethodSpec fromValueSpec() {
        return MethodSpec.methodBuilder("fromValue")
                         .returns(className)
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .addJavadoc("Use this in place of valueOf to convert the raw string returned by the service into the " +
                                     "enum value.\n\n" +
                                     "@param $N real value\n" +
                                     "@return $T corresponding to the value\n", VALUE, className)
                         .addParameter(String.class, VALUE)
                         .beginControlFlow("if ($N == null)", VALUE)
                         .addStatement("return null")
                         .endControlFlow()
                         .addStatement("return $1T.of($2T.values())\n" +
                                       ".filter(e -> e.toString().equals($3N))\n" +
                                       ".findFirst()\n" +
                                       ".orElse(UNKNOWN_TO_SDK_VERSION)",
                                       Stream.class,
                                       className,
                                       VALUE)
                         .build();
    }

    private MethodSpec knownValuesSpec() {
        return MethodSpec.methodBuilder("knownValues")
                         .returns(ParameterizedTypeName.get(ClassName.get(Set.class), className))
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .addJavadoc("Use this in place of {@link #values()} to return a {@link Set} of all values known to the "
                                     + "SDK.\n"
                                     + "This will return all known enum values except {@link #$N}.\n\n"
                                     + "@return a {@link $T} of known {@link $T}s", UNKNOWN_TO_SDK_VERSION, Set.class, className)
                         .addStatement("return $T.of(values()).filter(v -> v != $N).collect(toSet())",
                                       Stream.class,
                                       UNKNOWN_TO_SDK_VERSION)
                         .build();
    }
}
