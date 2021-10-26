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
import com.squareup.javapoet.TypeSpec;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.lite.PoetClass;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Generates DefaultsModeConfiguration class that contains default options for each mode
 */
public class DefaultsModeConfigurationGenerator implements PoetClass {

    private final String basePackage;
    private final String defaultsModeBase;
    private final DefaultConfiguration configuration;

    private static final Map<String, OptionMetadata> CONFIGURATION_MAPPING = new HashMap<>();

    private static final Map<String, OptionMetadata> HTTP_CONFIGURATION_MAPPING = new HashMap<>();

    static {
        HTTP_CONFIGURATION_MAPPING.put("connectTimeoutInMillis",
                                       new OptionMetadata(ClassName.get("java.time", "Duration"),
                                                          ClassName.get("software.amazon.awssdk.http",
                                                                        "SdkHttpConfigurationOption", "CONNECTION_TIMEOUT")));
        CONFIGURATION_MAPPING.put("retryMode", new OptionMetadata(ClassName.get("software.amazon.awssdk.core.retry", "RetryMode"
        ), ClassName.get("software.amazon.awssdk.core.client.config","SdkClientOption", "DEFAULT_RETRY_MODE")));
    }

    public DefaultsModeConfigurationGenerator(String basePackage, String defaultsModeBase, DefaultConfiguration configuration) {
        this.basePackage = basePackage;
        this.configuration = configuration;
        this.defaultsModeBase = defaultsModeBase;
    }

    @Override
    public TypeSpec poetClass() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(className())
                                           .addModifiers(PUBLIC, FINAL)
                                           .addJavadoc(documentation())
                                           .addAnnotation(SdkInternalApi.class)
                                           .addAnnotation(AnnotationSpec.builder(Generated.class)
                                                                        .addMember("value",
                                                                                   "$S",
                                                                                   "software.amazon.awssdk:codegen")
                                                                        .build())
                                           .addMethod(defaultHttpConfigMethod(configuration.modeDefaults().keySet()))
                                           .addMethod(defaultSdkConfigMethod(configuration.modeDefaults().keySet()))
                                           .addMethod(createConstructor());


        configuration.modeDefaults().entrySet().forEach(entry -> {
            builder.addField(addDefaultsFieldForMode(entry));
            builder.addField(addHttpDefaultsFieldForMode(entry));
        });

        addDefaultsFieldForLegacy(builder, "LEGACY_DEFAULTS");
        addDefaultsFieldForLegacy(builder, "LEGACY_HTTP_DEFAULTS");
        return builder.build();
    }

    @Override
    public ClassName className() {
        return ClassName.get(basePackage, "DefaultsModeConfiguration");
    }

    private FieldSpec addDefaultsFieldForMode(Map.Entry<String, Map<String, String>> modeEntry) {
        String mode = modeEntry.getKey();
        String fieldName = sanitizeMode(mode) + "_DEFAULTS";


        CodeBlock.Builder attributeBuilder = CodeBlock.builder()
                                                      .add("$T.builder()", AttributeMap.class);

        modeEntry.getValue()
                 .entrySet()
                 .stream()
                 .filter(e -> CONFIGURATION_MAPPING.containsKey(e.getKey()))
                 .forEach(e -> attributeMapBuilder(e.getKey(), e.getValue(), attributeBuilder));


        FieldSpec.Builder fieldSpec = FieldSpec.builder(AttributeMap.class, fieldName, PRIVATE, STATIC, FINAL)
                                               .initializer(attributeBuilder
                                                                .add(".build()")
                                                                .build());


        return fieldSpec.build();
    }

    private void addDefaultsFieldForLegacy(TypeSpec.Builder builder, String name) {
        FieldSpec field = FieldSpec.builder(AttributeMap.class, name, PRIVATE, STATIC, FINAL)
                                   .initializer("$T.empty()", AttributeMap.class).build();
        builder.addField(field);
    }

    private void attributeMapBuilder(String option, String value, CodeBlock.Builder attributeBuilder) {
        OptionMetadata optionMetadata = CONFIGURATION_MAPPING.get(option);
        switch (option) {
            case "retryMode":
                attributeBuilder.add(".put($T, $T.$N)", optionMetadata.attribute, optionMetadata.type, value.toUpperCase(Locale.US));
                break;
            default:
                throw new IllegalStateException("Unsupported option " + option);
        }
    }

    private void httpAttributeMapBuilder(String option, String value, CodeBlock.Builder attributeBuilder) {
        OptionMetadata optionMetadata = HTTP_CONFIGURATION_MAPPING.get(option);
        switch (option) {
            case "connectTimeoutInMillis":
                attributeBuilder.add(".put($T, $T.ofMillis($N))", optionMetadata.attribute, optionMetadata.type, value);
                break;
            default:
                throw new IllegalStateException("Unsupported option " + option);
        }
    }

    private FieldSpec addHttpDefaultsFieldForMode(Map.Entry<String, Map<String, String>> modeEntry) {
        String mode = modeEntry.getKey();
        String fieldName = sanitizeMode(mode) + "_HTTP_DEFAULTS";

        CodeBlock.Builder attributeBuilder = CodeBlock.builder()
                                                      .add("$T.builder()", AttributeMap.class);

        modeEntry.getValue()
                 .entrySet()
                 .stream()
                 .filter(e -> HTTP_CONFIGURATION_MAPPING.containsKey(e.getKey()))
                 .forEach(e -> httpAttributeMapBuilder(e.getKey(), e.getValue(), attributeBuilder));

        FieldSpec.Builder fieldSpec = FieldSpec.builder(AttributeMap.class, fieldName, PRIVATE, STATIC, FINAL)
                                               .initializer(attributeBuilder
                                                                .add(".build()")
                                                                .build());

        return fieldSpec.build();
    }

    private String sanitizeMode(String str) {
        return str.replace('-', '_').toUpperCase(Locale.US);
    }

    private CodeBlock documentation() {
        CodeBlock.Builder builder = CodeBlock.builder()
                                             .add("Contains a collection of default configuration options for each "
                                                  + "DefaultsMode");

        return builder.build();
    }


    private MethodSpec defaultHttpConfigMethod(Set<String> modes) {
        String nameSuffix = "_HTTP_DEFAULTS";
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("defaultHttpConfig")
                                                     .returns(AttributeMap.class)
                                                     .addModifiers(PUBLIC, STATIC)
                                                     .addJavadoc("Return the default HTTP config options for a given defaults "
                                                                 + "mode")
                                                     .addParameter(defaultsModeClassName(), "mode")
                                                     .beginControlFlow("switch (mode)");


        addSwitchCaseForEachMode(modes, nameSuffix, methodBuilder);

        addLegacyCase(methodBuilder, "LEGACY" + nameSuffix);

        return methodBuilder
            .addStatement("default: throw new IllegalArgumentException($S + $N)", "Unsupported mode: ", "mode")
            .endControlFlow()
            .build();
    }

    private void addSwitchCaseForEachMode(Set<String> modes, String nameSuffix, MethodSpec.Builder methodBuilder) {
        modes.forEach(m -> {
            String mode = sanitizeMode(m);
            methodBuilder.addCode("case $N:", mode);
            methodBuilder.addStatement("return $N", mode + nameSuffix);
        });
    }

    private MethodSpec defaultSdkConfigMethod(Set<String> modes) {
        String nameSuffix = "_DEFAULTS";
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("defaultConfig")
                                                     .returns(AttributeMap.class)
                                                     .addModifiers(PUBLIC, STATIC)
                                                     .addJavadoc("Return the default SDK config options for a given defaults "
                                                                 + "mode")
                                                     .addParameter(defaultsModeClassName(), "mode")
                                                     .beginControlFlow("switch (mode)");


        addSwitchCaseForEachMode(modes, nameSuffix, methodBuilder);
        addLegacyCase(methodBuilder, "LEGACY" + nameSuffix);

        return methodBuilder
            .addStatement("default: throw new IllegalArgumentException($S + $N)", "Unsupported mode: ", "mode")
            .endControlFlow()
            .build();
    }

    private void addLegacyCase(MethodSpec.Builder methodBuilder, String name) {
        methodBuilder.addCode("case LEGACY:");
        methodBuilder.addStatement("return $N", name);
    }

    private ClassName defaultsModeClassName() {
        return ClassName.get(defaultsModeBase, "DefaultsMode");
    }

    private MethodSpec createConstructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PRIVATE)
                         .build();
    }

    private static final class OptionMetadata {
        private final ClassName type;
        private final ClassName attribute;

        public OptionMetadata(ClassName type, ClassName attribute) {
            this.type = type;
            this.attribute = attribute;
        }
    }
}
