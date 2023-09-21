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

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsServiceClientConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeSpecUtils;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.config.internal.SdkClientConfigurationUtil;
import software.amazon.awssdk.core.client.config.internal.SdkInternalAdvancedClientOption;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;

public class ServiceClientConfigurationClass implements ClassSpec {
    private final ClassName defaultClientMetadataClassName;
    private final AuthSchemeSpecUtils authSchemeSpecUtils;

    public ServiceClientConfigurationClass(IntermediateModel model) {
        String basePackage = model.getMetadata().getFullClientPackageName();
        String serviceId = model.getMetadata().getServiceName();
        this.defaultClientMetadataClassName = ClassName.get(basePackage, serviceId + "ServiceClientConfiguration");
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(model);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(defaultClientMetadataClassName)
                                            .superclass(AwsServiceClientConfiguration.class)
                                            .addJavadoc("Class to expose the service client settings to the user. "
                                                        + "Implementation of {@link $T}",
                                                        AwsServiceClientConfiguration.class);

        builder.addMethod(constructor());
        for (Field field : serviceClientConfigurationFields()) {
            addLocalFieldForDataIfNeeded(field, builder);
            if (field.isLocalField()) {
                builder.addMethod(getterForDataField(field));
            }
        }

        return builder.addMethod(builderMethod())
                      .addMethod(builderFromSdkClientConfiguration())
                      .addModifiers(PUBLIC, FINAL)
                      .addAnnotation(SdkPublicApi.class)
                      .addType(builderInterfaceSpec())
                      .addType(builderInternalInterfaceSpec())
                      .addType(builderImplSpec())
                      .build();
    }

    @Override
    public ClassName className() {
        return defaultClientMetadataClassName;
    }

    private MethodSpec constructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                                               .addModifiers(PRIVATE)
                                               .addParameter(className().nestedClass("Builder"), "builder");
        builder.addStatement("super(builder)");
        for (Field field : serviceClientConfigurationFields()) {
            if (field.isLocalField()) {
                builder.addStatement("this.$L = builder.$L()", field.name, field.name);
            }
        }
        return builder.build();
    }

    private MethodSpec builderMethod() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(PUBLIC, STATIC)
                         .addStatement("return new BuilderImpl()")
                         .returns(className().nestedClass("Builder"))
                         .addJavadoc("")
                         .build();
    }

    private MethodSpec builderFromSdkClientConfiguration() {
        return MethodSpec.methodBuilder("builder")
                         .addModifiers(PUBLIC, STATIC)
                         .addParameter(SdkClientConfiguration.Builder.class, "builder")
                         .returns(className().nestedClass("BuilderInternal"))
                         .addStatement("return new BuilderImpl(builder)")
                         .build();
    }

    private TypeSpec builderInterfaceSpec() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("Builder")
                                           .addModifiers(PUBLIC)
                                           .addSuperinterface(ClassName.get(AwsServiceClientConfiguration.class).nestedClass(
                                               "Builder"))
                                           .addJavadoc("A builder for creating a {@link $T}", className());
        for (Field field : serviceClientConfigurationFields()) {
            builder.addMethod(baseSetterForField(field)
                                  .addModifiers(ABSTRACT)
                                  .build());
            builder.addMethod(baseGetterForField(field)
                                  .addModifiers(ABSTRACT)
                                  .build());
        }
        builder.addMethod(MethodSpec.methodBuilder("build")
                                    .addAnnotation(Override.class)
                                    .addModifiers(PUBLIC, ABSTRACT)
                                    .returns(className())
                                    .build());
        return builder.build();
    }

    private TypeSpec builderInternalInterfaceSpec() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("BuilderInternal")
                                           .addModifiers(PUBLIC)
                                           .addSuperinterface(className().nestedClass("Builder"))
                                           .addJavadoc("An internal builder for creating a {@link $T}", className());

        builder.addMethod(MethodSpec.methodBuilder("buildSdkClientConfiguration")
                                    .addModifiers(PUBLIC, ABSTRACT)
                                    .returns(SdkClientConfiguration.class)
                                    .build());
        return builder.build();
    }

    private TypeSpec builderImplSpec() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("BuilderImpl")
                                           .addModifiers(PRIVATE, STATIC, FINAL)
                                           .addSuperinterface(className().nestedClass("BuilderInternal"));

        builder.addField(SdkClientConfiguration.Builder.class, "builder", PRIVATE, FINAL);

        builder.addMethod(MethodSpec.constructorBuilder()
                                    .addModifiers(PRIVATE)
                                    .addStatement("this.builder = $T.builder()", SdkClientConfiguration.class)
                                    .build());

        builder.addMethod(MethodSpec.constructorBuilder()
                                    .addModifiers(PRIVATE)
                                    .addParameter(SdkClientConfiguration.Builder.class, "builder")
                                    .addStatement("this.builder = builder", SdkClientConfiguration.class)
                                    .build());
        for (Field field : serviceClientConfigurationFields()) {
            addLocalFieldForBuilderIfNeeded(field, builder);
            builder.addMethod(setterForField(field));
            builder.addMethod(getterForBuilderField(field));
        }

        builder.addMethod(MethodSpec.methodBuilder("build")
                                    .addAnnotation(Override.class)
                                    .addModifiers(PUBLIC)
                                    .returns(className())
                                    .addStatement("return new $T(this)", className())
                                    .build());

        builder.addMethod(MethodSpec.methodBuilder("buildSdkClientConfiguration")
                                    .addModifiers(PUBLIC)
                                    .addAnnotation(Override.class)
                                    .returns(SdkClientConfiguration.class)
                                    .addStatement("$T overrideConfiguration = overrideConfiguration()",
                                                  ClientOverrideConfiguration.class)
                                    .beginControlFlow("if (overrideConfiguration != null)")
                                    .addStatement("$T.copyOverridesToConfiguration(overrideConfiguration, builder)",
                                                  SdkClientConfigurationUtil.class)
                                    .endControlFlow()
                                    .addStatement("return builder.build()")
                                    .build());
        return builder.build();
    }

    private void addLocalFieldForBuilderIfNeeded(Field field, TypeSpec.Builder builder) {
        if (field.optionClass == null) {
            builder.addField(field.type, field.name, PRIVATE);
        }
    }

    private void addLocalFieldForDataIfNeeded(Field field, TypeSpec.Builder builder) {
        if (field.isLocalField()) {
            builder.addField(field.type, field.name, PRIVATE, FINAL);
        }
    }

    private MethodSpec setterForField(Field field) {
        MethodSpec.Builder builder = baseSetterForField(field);
        if (field.isLocalField()) {
            builder.addAnnotation(Override.class);
        }
        if (field.optionClass == null) {
            return builder.addStatement("this.$1L = $1L", field.name)
                          .addStatement("return this")
                          .build();

        }
        return builder.addStatement("builder.option($T.$L, $L)",
                                    field.optionClass, field.optionName, field.name)
                      .addStatement("return this")
                      .build();
    }

    private MethodSpec.Builder baseSetterForField(Field field) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(field.name)
                                               .addModifiers(PUBLIC)
                                               .addParameter(field.type, field.name)
                                               .addJavadoc("Sets the value for " + field.doc)
                                               .returns(className().nestedClass("Builder"));
        if (!field.isLocalField()) {
            builder.addAnnotation(Override.class);
        }
        return builder;
    }

    private MethodSpec getterForBuilderField(Field field) {
        return getterForField(field, "builder", false);
    }

    private MethodSpec getterForDataField(Field field) {
        return getterForField(field, "config", true);
    }

    private MethodSpec getterForField(Field field, String fieldName, boolean forDataGetter) {
        MethodSpec.Builder builder = baseGetterForField(field);
        if (!forDataGetter && field.isLocalField()) {
            builder.addAnnotation(Override.class);
        }
        if (forDataGetter && field.isLocalField()) {
            return builder.addStatement("return $L", field.name)
                          .build();
        }
        if (field.optionClass == null) {
            return builder.addStatement("return $L", field.name)
                          .build();
        }
        if (field.baseType != null) {
            return builder.addStatement("$T result = $L.option($T.$L)",
                                        field.baseType, fieldName, field.optionClass, field.optionName)
                          .beginControlFlow("if (result == null)")
                          .addStatement("return null")
                          .endControlFlow()
                          .addStatement("return $T.isInstanceOf($T.class, result, $S + $T.class.getSimpleName())",
                                        Validate.class, field.type,
                                        "Expected an instance of ", field.type)
                          .build();
        }
        return builder.addStatement("return $L.option($T.$L)", fieldName, field.optionClass, field.optionName)
                      .build();
    }

    private MethodSpec.Builder baseGetterForField(Field field) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(field.name)
                                               .addModifiers(PUBLIC)
                                               .addJavadoc("Gets the value for " + field.doc)
                                               .returns(field.type);
        if (!field.isLocalField()) {
            builder.addAnnotation(Override.class);
        }
        return builder;
    }


    /**
     * Returns the list of fields present in the service client configuration class with its corresponding {@link ClientOption}
     * mapping to the {@link SdkClientConfiguration} class.
     */
    private List<Field> serviceClientConfigurationFields() {
        List<Field> fields = new ArrayList<>(baseServiceClientConfigurationFields());
        fields.add(Field.builder("authSchemeProvider", authSchemeSpecUtils.providerInterfaceName())
                        .doc("auth scheme provider")
                        .optionClass(SdkClientOption.class)
                        .optionValue(SdkClientOption.AUTH_SCHEME_PROVIDER)
                        .baseType(ClassName.get(AuthSchemeProvider.class))
                        .build());
        return fields;
    }

    private static List<Field> baseServiceClientConfigurationFields() {
        return Arrays.asList(
            Field.builder("overrideConfiguration", ClientOverrideConfiguration.class)
                 .doc("client override configuration")
                 .definingClass(SdkServiceClientConfiguration.class)
                 .optionClass(SdkInternalAdvancedClientOption.class)
                 .optionValue(SdkInternalAdvancedClientOption.CLIENT_OVERRIDE_CONFIGURATION)
                 .build(),
            Field.builder("endpointOverride", URI.class)
                 .doc("endpoint override")
                 .definingClass(SdkServiceClientConfiguration.class)
                 .optionClass(SdkInternalAdvancedClientOption.class)
                 .optionValue(SdkInternalAdvancedClientOption.ENDPOINT_OVERRIDE_VALUE)
                 .build(),
            Field.builder("endpointProvider", EndpointProvider.class)
                 .doc("endpoint provider")
                 .definingClass(SdkServiceClientConfiguration.class)
                 .optionClass(SdkClientOption.class)
                 .optionValue(SdkClientOption.ENDPOINT_PROVIDER)
                 .build(),
            Field.builder("region", Region.class)
                 .doc("AWS region")
                 .definingClass(AwsServiceClientConfiguration.class)
                 .optionClass(AwsClientOption.class)
                 .optionValue(AwsClientOption.AWS_REGION)
                 .build(),
            Field.builder("credentialsProvider",
                          ParameterizedTypeName.get(ClassName.get(IdentityProvider.class),
                                                    WildcardTypeName.subtypeOf(AwsCredentialsIdentity.class)))
                 .doc("credentials provider")
                 .definingClass(AwsServiceClientConfiguration.class)
                 .optionClass(AwsClientOption.class)
                 .optionValue(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER)
                 .build()
        );
    }

    static class Field {
        private final String name;
        private final TypeName type;
        private final Class<? extends SdkServiceClientConfiguration> definingClass;
        private final Class<? extends ClientOption> optionClass;
        private final String optionName;
        private final String doc;
        private final TypeName baseType;

        Field(Builder builder) {
            this.name = Validate.paramNotNull(builder.name, "name");
            this.type = Validate.paramNotNull(builder.type, "type");
            this.definingClass = builder.definingClass;
            this.doc = Validate.paramNotNull(builder.doc, "doc");
            this.optionClass = builder.optionClass;
            this.optionName = builder.optionName;
            this.baseType = builder.baseType;
        }

        public boolean isLocalField() {
            return definingClass == null;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static Builder builder(String name, TypeName type) {
            return new Builder()
                .name(name)
                .type(type);
        }

        public static Builder builder(String name, Class<?> type) {
            return new Builder()
                .name(name)
                .type(type);
        }

        static class Builder {
            private String name;
            private TypeName type;
            private String doc;
            private Class<? extends SdkServiceClientConfiguration> definingClass;
            private Class<? extends ClientOption> optionClass;
            private ClientOption<?> value;
            private String optionName;
            private TypeName baseType;

            public Field.Builder name(String name) {
                this.name = name;
                return this;
            }

            public Field.Builder type(Class<?> type) {
                this.type = ClassName.get(type);
                return this;
            }

            public Field.Builder type(TypeName type) {
                this.type = type;
                return this;
            }

            public Field.Builder doc(String doc) {
                this.doc = doc;
                return this;
            }

            public Field.Builder optionClass(Class<? extends ClientOption> optionClass) {
                this.optionClass = optionClass;
                return this;
            }

            public Field.Builder optionValue(ClientOption<?> value) {
                this.value = value;
                return this;
            }

            public Field.Builder baseType(TypeName baseType) {
                this.baseType = baseType;
                return this;
            }

            public Field.Builder definingClass(Class<? extends SdkServiceClientConfiguration> definingClass) {
                this.definingClass = definingClass;
                return this;
            }

            public Field build() {
                if (value != null && optionClass != null) {
                    optionName = getFieldName(value, optionClass);
                }
                return new Field(this);
            }
        }

        // This method resolves an static reference to its name, for instance, when called with
        //
        // getFieldName(AwsClientOption.AWS_REGION, AwsClientOption.class)
        //
        // it will return the string "AWS_REGION" that we can use for codegen.
        // Using the value directly avoid typo bugs and allows the compiler and the IDE to know about this relationship.
        //
        // This method uses the fully qualified names in the reflection package to avoid polluting this class imports.
        // Adapted from https://stackoverflow.com/a/35416606
        private static String getFieldName(Object fieldObject, Class<?> parent) {
            java.lang.reflect.Field[] allFields = parent.getFields();
            for (java.lang.reflect.Field field : allFields) {
                int modifiers = field.getModifiers();
                if (!java.lang.reflect.Modifier.isStatic(modifiers)) {
                    continue;
                }
                Object currentFieldObject;
                try {
                    // For static fields you can pass a null to get back its value.
                    currentFieldObject = field.get(null);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
                boolean isWantedField = fieldObject.equals(currentFieldObject);
                if (isWantedField) {
                    return field.getName();
                }
            }
            throw new java.util.NoSuchElementException(String.format("cannot find constant %s in class %s",
                                                                     fieldObject,
                                                                     parent.getClass().getName()));
        }
    }
}
