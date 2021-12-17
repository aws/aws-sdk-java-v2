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
import java.util.EnumMap;
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

    private static final String DEFAULT_CONFIG_BY_MODE_ENUM_MAP = "DEFAULT_CONFIG_BY_MODE";
    private static final String DEFAULT_HTTP_CONFIG_BY_MODE_ENUM_MAP = "DEFAULT_HTTP_CONFIG_BY_MODE";
    private static final String DEFAULTS_VAR_SUFFIX = "_DEFAULTS";
    private static final String HTTP_DEFAULTS_VAR_SUFFIX = "_HTTP_DEFAULTS";
    private static final Map<String, OptionMetadata> CONFIGURATION_MAPPING = new HashMap<>();
    private static final Map<String, OptionMetadata> HTTP_CONFIGURATION_MAPPING = new HashMap<>();
    private static final String CONNECT_TIMEOUT_IN_MILLIS = "connectTimeoutInMillis";
    private static final String TLS_NEGOTIATION_TIMEOUT_IN_MILLIS = "tlsNegotiationTimeoutInMillis";
    private static final String S3_US_EAST_1_REGIONAL_ENDPOINTS = "s3UsEast1RegionalEndpoints";

    private final String basePackage;
    private final String defaultsModeBase;
    private final DefaultConfiguration configuration;

    static {
        HTTP_CONFIGURATION_MAPPING.put(CONNECT_TIMEOUT_IN_MILLIS,
                                       new OptionMetadata(ClassName.get("java.time", "Duration"),
                                                          ClassName.get("software.amazon.awssdk.http",
                                                                        "SdkHttpConfigurationOption", "CONNECTION_TIMEOUT")));
        HTTP_CONFIGURATION_MAPPING.put(TLS_NEGOTIATION_TIMEOUT_IN_MILLIS,
                                       new OptionMetadata(ClassName.get("java.time", "Duration"),
                                                          ClassName.get("software.amazon.awssdk.http",
                                                                        "SdkHttpConfigurationOption",
                                                                        "TLS_NEGOTIATION_TIMEOUT")));
        CONFIGURATION_MAPPING.put("retryMode", new OptionMetadata(ClassName.get("software.amazon.awssdk.core.retry", "RetryMode"
        ), ClassName.get("software.amazon.awssdk.core.client.config", "SdkClientOption", "DEFAULT_RETRY_MODE")));

        CONFIGURATION_MAPPING.put(S3_US_EAST_1_REGIONAL_ENDPOINTS,
                                  new OptionMetadata(ClassName.get(String.class),
                                                     ClassName.get("software.amazon.awssdk.regions",
                                                                   "ServiceMetadataAdvancedOption",
                                                                   "DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT")));
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
                                           .addMethod(defaultConfigMethod(DEFAULT_CONFIG_BY_MODE_ENUM_MAP, "defaultConfig"))
                                           .addMethod(defaultConfigMethod(DEFAULT_HTTP_CONFIG_BY_MODE_ENUM_MAP,
                                                                          "defaultHttpConfig"))
                                           .addMethod(createConstructor());


        configuration.modeDefaults().entrySet().forEach(entry -> {
            builder.addField(addDefaultsFieldForMode(entry));
            builder.addField(addHttpDefaultsFieldForMode(entry));
        });

        addDefaultsFieldForLegacy(builder, "LEGACY_DEFAULTS");
        addDefaultsFieldForLegacy(builder, "LEGACY_HTTP_DEFAULTS");

        addEnumMapField(builder, DEFAULT_CONFIG_BY_MODE_ENUM_MAP);
        addEnumMapField(builder, DEFAULT_HTTP_CONFIG_BY_MODE_ENUM_MAP);

        addStaticEnumMapBlock(builder);
        return builder.build();
    }

    private void addStaticEnumMapBlock(TypeSpec.Builder builder) {
        CodeBlock.Builder staticCodeBlock = CodeBlock.builder();

        putItemsToEnumMap(staticCodeBlock, configuration.modeDefaults().keySet(), DEFAULTS_VAR_SUFFIX,
                          DEFAULT_CONFIG_BY_MODE_ENUM_MAP);
        putItemsToEnumMap(staticCodeBlock, configuration.modeDefaults().keySet(), HTTP_DEFAULTS_VAR_SUFFIX,
                          DEFAULT_HTTP_CONFIG_BY_MODE_ENUM_MAP);

        builder.addStaticBlock(staticCodeBlock.build());
    }

    private void addEnumMapField(TypeSpec.Builder builder, String name) {
        ParameterizedTypeName map = ParameterizedTypeName.get(ClassName.get(Map.class),
                                                              defaultsModeClassName(),
                                                              ClassName.get(AttributeMap.class));
        FieldSpec field = FieldSpec.builder(map, name, PRIVATE, STATIC, FINAL)
                                   .initializer("new $T<>(DefaultsMode.class)", EnumMap.class).build();
        builder.addField(field);
    }

    private void putItemsToEnumMap(CodeBlock.Builder codeBlock, Set<String> modes, String suffix, String mapName) {
        modes.forEach(m -> {
            String mode = sanitizeMode(m);
            codeBlock.addStatement("$N.put(DefaultsMode.$N, $N)", mapName, mode, mode + suffix);
        });

        // Add LEGACY since LEGACY is not in the modes set
        codeBlock.addStatement("$N.put(DefaultsMode.LEGACY, LEGACY$N)", mapName, suffix);
    }

    @Override
    public ClassName className() {
        return ClassName.get(basePackage, "DefaultsModeConfiguration");
    }

    private FieldSpec addDefaultsFieldForMode(Map.Entry<String, Map<String, String>> modeEntry) {
        String mode = modeEntry.getKey();
        String fieldName = sanitizeMode(mode) + DEFAULTS_VAR_SUFFIX;

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
                attributeBuilder.add(".put($T, $T.$N)", optionMetadata.attribute, optionMetadata.type,
                                     value.toUpperCase(Locale.US));
                break;
            case S3_US_EAST_1_REGIONAL_ENDPOINTS:
                attributeBuilder.add(".put($T, $S)", optionMetadata.attribute, value);
                break;
            default:
                throw new IllegalStateException("Unsupported option " + option);
        }
    }

    private void httpAttributeMapBuilder(String option, String value, CodeBlock.Builder attributeBuilder) {
        OptionMetadata optionMetadata = HTTP_CONFIGURATION_MAPPING.get(option);
        switch (option) {
            case CONNECT_TIMEOUT_IN_MILLIS:
            case TLS_NEGOTIATION_TIMEOUT_IN_MILLIS:
                attributeBuilder.add(".put($T, $T.ofMillis($N))", optionMetadata.attribute, optionMetadata.type, value);
                break;
            default:
                throw new IllegalStateException("Unsupported option " + option);
        }
    }

    private FieldSpec addHttpDefaultsFieldForMode(Map.Entry<String, Map<String, String>> modeEntry) {
        String mode = modeEntry.getKey();
        String fieldName = sanitizeMode(mode) + HTTP_DEFAULTS_VAR_SUFFIX;

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

    private MethodSpec defaultConfigMethod(String enumMap, String methodName) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                                                     .returns(AttributeMap.class)
                                                     .addModifiers(PUBLIC, STATIC)
                                                     .addJavadoc("Return the default config options for a given defaults "
                                                                 + "mode")
                                                     .addParameter(defaultsModeClassName(), "mode")
                                                     .addStatement("return $N.getOrDefault(mode, $T.empty())",
                                                                   enumMap, AttributeMap.class);

        return methodBuilder.build();
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

        OptionMetadata(ClassName type, ClassName attribute) {
            this.type = type;
            this.attribute = attribute;
        }
    }
}
