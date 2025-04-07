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

package software.amazon.awssdk.codegen.poet.common;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static software.amazon.awssdk.codegen.poet.PoetUtils.createEnumBuilder;
import static software.amazon.awssdk.codegen.poet.PoetUtils.toStringBuilder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.utils.internal.EnumUtils;

public abstract class AbstractEnumClass implements ClassSpec {

    private static final String VALUE = "value";
    private static final String VALUE_MAP = "VALUE_MAP";
    private static final String UNKNOWN_TO_SDK_VERSION = "UNKNOWN_TO_SDK_VERSION";
    private final ShapeModel shape;

    public AbstractEnumClass(ShapeModel shape) {
        this.shape = shape;
    }

    @Override
    public final TypeSpec poetSpec() {
        Builder enumBuilder = createEnumBuilder(className())
                .addField(valueMapField())
                .addField(String.class, VALUE, Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(toStringBuilder().addStatement("return $T.valueOf($N)", String.class, VALUE).build())
                .addMethod(fromValueSpec())
                .addMethod(knownValuesSpec())
                .addMethod(createConstructor());

        addSuperInterface(enumBuilder);
        addDeprecated(enumBuilder);
        addJavadoc(enumBuilder);
        addEnumConstants(enumBuilder);

        enumBuilder.addEnumConstant(UNKNOWN_TO_SDK_VERSION, TypeSpec.anonymousClassBuilder("null").build());

        addAdditionalMethods(enumBuilder);
        return enumBuilder.build();
    }

    protected final ShapeModel getShape() {
        return shape;
    }

    protected abstract void addDeprecated(Builder enumBuilder);

    protected abstract void addJavadoc(Builder enumBuilder);

    protected abstract void addEnumConstants(Builder enumBuilder);

    protected void addSuperInterface(Builder enumBuilder) {
        // no-op
    }

    protected void addAdditionalMethods(Builder enumBuilder) {
        // no-op
    }
    
    private FieldSpec valueMapField() {
        ParameterizedTypeName mapType = ParameterizedTypeName.get(ClassName.get(Map.class),
                                                                  ClassName.get(String.class),
                                                                  className());
        return FieldSpec.builder(mapType, VALUE_MAP)
                        .addModifiers(PRIVATE, STATIC, FINAL)
                        .initializer("$1T.uniqueIndex($2T.class, $2T::toString)", EnumUtils.class, className())
                        .build();
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
                .returns(className())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Use this in place of valueOf to convert the raw string returned by the service into the " +
                        "enum value.\n\n" +
                        "@param $N real value\n" +
                        "@return $T corresponding to the value\n", VALUE, className())
                .addParameter(String.class, VALUE)
                .beginControlFlow("if ($N == null)", VALUE)
                .addStatement("return null")
                .endControlFlow()
                .addStatement("return $N.getOrDefault($N, $N)", VALUE_MAP, VALUE, UNKNOWN_TO_SDK_VERSION)
                .build();
    }

    private MethodSpec knownValuesSpec() {
        return MethodSpec.methodBuilder("knownValues")
                .returns(ParameterizedTypeName.get(ClassName.get(Set.class), className()))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Use this in place of {@link #values()} to return a {@link Set} of all values known to the "
                        + "SDK.\n"
                        + "This will return all known enum values except {@link #$N}.\n\n"
                        + "@return a {@link $T} of known {@link $T}s", UNKNOWN_TO_SDK_VERSION, Set.class, className())
                .addStatement("$1T<$2T> knownValues = $3T.allOf($2T.class)", Set.class, className(), EnumSet.class)
                .addStatement("knownValues.remove($N)", UNKNOWN_TO_SDK_VERSION)
                .addStatement("return knownValues")
                .build();
    }
}
