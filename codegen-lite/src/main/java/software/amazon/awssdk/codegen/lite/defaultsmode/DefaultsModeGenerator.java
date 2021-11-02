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

package software.amazon.awssdk.codegen.lite.defaultsmode;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Locale;
import java.util.Map;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.lite.PoetClass;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.internal.EnumUtils;

/**
 * Generates DefaultsMode enum
 */
public class DefaultsModeGenerator implements PoetClass {

    private static final String VALUE = "value";
    private static final String VALUE_MAP = "VALUE_MAP";

    private final String basePackage;
    private final DefaultConfiguration configuration;

    public DefaultsModeGenerator(String basePackage, DefaultConfiguration configuration) {
        this.basePackage = basePackage;
        this.configuration = configuration;
    }

    @Override
    public TypeSpec poetClass() {
        TypeSpec.Builder builder = TypeSpec.enumBuilder(className())
                                           .addField(valueMapField())
                                           .addField(String.class, VALUE, Modifier.PRIVATE, Modifier.FINAL)
                                           .addModifiers(PUBLIC)
                                           .addJavadoc(documentation())
                                           .addAnnotation(SdkPublicApi.class)
                                           .addAnnotation(AnnotationSpec.builder(Generated.class)
                                                                        .addMember(VALUE,
                                                                                   "$S",
                                                                                   "software.amazon.awssdk:codegen")
                                                                        .build())
                                           .addMethod(fromValueSpec())
                                           .addMethod(toStringBuilder().addStatement("return $T.valueOf($N)", String.class,
                                                                                     VALUE).build())
                                           .addMethod(createConstructor());

        builder.addEnumConstant("LEGACY", enumValueTypeSpec("legacy", javaDocForMode("legacy")));

        configuration.modeDefaults().keySet().forEach(k -> {
            String enumKey = sanitizeEnum(k);
            builder.addEnumConstant(enumKey, enumValueTypeSpec(k, javaDocForMode(k)));
        });

        builder.addEnumConstant("AUTO", enumValueTypeSpec("auto", javaDocForMode("auto")));

        return builder.build();
    }

    @Override
    public ClassName className() {
        return ClassName.get(basePackage, "DefaultsMode");
    }

    private TypeSpec enumValueTypeSpec(String value, String documentation) {
        return TypeSpec.anonymousClassBuilder("$S", value)
                       .addJavadoc(documentation)
                       .build();
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

    private String sanitizeEnum(String str) {
        return str.replace('-', '_').toUpperCase(Locale.US);
    }

    private String javaDocForMode(String mode) {
        return configuration.modesDocumentation().getOrDefault(mode, "");
    }

    private CodeBlock documentation() {
        CodeBlock.Builder builder = CodeBlock.builder()
                                             .add("A defaults mode determines how certain default configuration options are "
                                                  + "resolved in "
                                                  + "the SDK. "
                                                  + "Based on the provided "
                                                  + "mode, the SDK will vend sensible default values tailored to the mode for "
                                                  + "the following settings:")
                                             .add(System.lineSeparator());

        builder.add("<ul>");
        configuration.configurationDocumentation().forEach((k, v) -> {
            builder.add("<li>" + k + ": " + v + "</li>");
        });
        builder.add("</ul>").add(System.lineSeparator());

        builder.add("<p>All options above can be configured by users, and the overridden value will take precedence.")
               .add("<p><b>Note:</b> for any mode other than {@link #LEGACY}, the vended default values might change "
                    + "as best practices may evolve. As a result, it is encouraged to perform testing when upgrading the SDK if"
                    + " you are using a mode other than {@link #LEGACY}")
               .add(System.lineSeparator());

        return builder.add("<p>While the {@link #LEGACY} defaults mode is specific to Java, other modes are "
                           + "standardized across "
                           + "all of the AWS SDKs</p>")
                      .add(System.lineSeparator())
                      .add("<p>The defaults mode can be configured:")
                      .add(System.lineSeparator())
                      .add("<ol>")
                      .add("<li>Directly on a client via {@code AwsClientBuilder.Builder#defaultsMode"
                           + "(DefaultsMode)}.</li>")
                      .add(System.lineSeparator())
                      .add("<li>On a configuration profile via the \"defaults_mode\" profile file property.</li>")
                      .add(System.lineSeparator())
                      .add("<li>Globally via the \"aws.defaultsMode\" system property.</li>")
                      .add("<li>Globally via the \"AWS_DEFAULTS_MODE\" environment variable.</li>")
                      .add("</ol>")
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
                         .addStatement("$T.paramNotNull(value, $S)", Validate.class, VALUE)
                         .beginControlFlow("if (!VALUE_MAP.containsKey(value))")
                         .addStatement("throw new IllegalArgumentException($S + value)", "The provided value is not a"
                                                                                         + " valid "
                                                                                         + "defaults mode ")
                         .endControlFlow()
                         .addStatement("return $N.get($N)", VALUE_MAP, VALUE)
                         .build();
    }

    private MethodSpec createConstructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PRIVATE)
                         .addParameter(String.class, VALUE)
                         .addStatement("this.$1N = $1N", VALUE)
                         .build();
    }

    private static MethodSpec.Builder toStringBuilder() {
        return MethodSpec.methodBuilder("toString")
                         .returns(String.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class);
    }

}
