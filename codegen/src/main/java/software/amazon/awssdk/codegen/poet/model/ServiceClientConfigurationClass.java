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
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.SdkInternalAdvancedClientOption;
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
        return PoetUtils.createClassBuilder(defaultClientMetadataClassName)
                        .superclass(AwsServiceClientConfiguration.class)
                        .addJavadoc("Class to expose the service client settings to the user. Implementation of {@link $T}",
                                    AwsServiceClientConfiguration.class)
                        .addMethod(constructor())
                        .addMethod(builderMethod())
                        .addMethod(builderFromSdkClientConfiguration())
                        .addModifiers(PUBLIC, FINAL)
                        .addAnnotation(SdkPublicApi.class)
                        .addType(builderInterfaceSpec())
                        .addType(builderImplSpec())
                        .build();
    }

    @Override
    public ClassName className() {
        return defaultClientMetadataClassName;
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(PRIVATE)
                         .addParameter(className().nestedClass("Builder"), "builder")
                         .addStatement("super(builder)")
                         .build();
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
                         .addStatement("return new BuilderImpl(builder)")
                         .returns(className().nestedClass("Builder"))
                         .addJavadoc("")
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

    private TypeSpec builderImplSpec() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("BuilderImpl")
                                           .addModifiers(PRIVATE, STATIC, FINAL)
                                           .addSuperinterface(className().nestedClass("Builder"));

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
            addLocalFieldIfNeeded(field, builder);
            builder.addMethod(setterForField(field));
            builder.addMethod(getterForField(field));
        }

        builder.addMethod(MethodSpec.methodBuilder("build")
                                    .addAnnotation(Override.class)
                                    .addModifiers(PUBLIC)
                                    .returns(className())
                                    .addStatement("return new $T(this)", className())
                                    .build());

        builder.addMethod(MethodSpec.methodBuilder("buildSdkClientConfiguration")
                                    .addModifiers(PUBLIC)
                                    .returns(SdkClientConfiguration.class)
                                    .beginControlFlow("if (overrideConfiguration != null)")
                                    .addStatement("overrideConfiguration.addOverridesToConfiguration(builder)")
                                    .endControlFlow()
                                    .addStatement("return builder.build()")
                                    .build());
        return builder.build();
    }

    private void addLocalFieldIfNeeded(Field field, TypeSpec.Builder builder) {
        if (field.optionClass == null) {
            builder.addField(field.type, field.name, PRIVATE);
        }
    }

    private MethodSpec setterForField(Field field) {
        MethodSpec.Builder builder = baseSetterForField(field);
        if (!field.isInherited) {
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
        if (field.isInherited) {
            builder.addAnnotation(Override.class);
        }
        return builder;
    }

    private MethodSpec getterForField(Field field) {
        MethodSpec.Builder builder = baseGetterForField(field);
        if (!field.isInherited) {
            builder.addAnnotation(Override.class);
        }
        if (field.optionClass == null) {
            return builder.addStatement("return $L", field.name)
                          .build();
        }
        if (field.baseType != null) {
            return builder.addStatement("$T result = builder.option($T.$L)",
                                        field.baseType, field.optionClass, field.optionName)
                          .beginControlFlow("if (result == null)")
                          .addStatement("return null")
                          .endControlFlow()
                          .addStatement("return $T.isInstanceOf($T.class, result, $S + $T.class.getSimpleName())",
                                        Validate.class, field.type,
                                        "Expected an instance of ", field.type)
                          .build();

        }
        return builder.addStatement("return builder.option($T.$L)", field.optionClass, field.optionName)
                      .build();
    }

    private MethodSpec.Builder baseGetterForField(Field field) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(field.name)
                                               .addModifiers(PUBLIC)
                                               .addJavadoc("Gets the value for " + field.doc)
                                               .returns(field.type);
        if (field.isInherited) {
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
        fields.add(Field.builder()
                        .name("authSchemeProvider")
                        .type(authSchemeSpecUtils.providerInterfaceName())
                        .doc("auth scheme provider")
                        .optionClass(SdkClientOption.class)
                        .optionName("AUTH_SCHEME_PROVIDER")
                        .isInherited(false)
                        .baseType(ClassName.get(AuthSchemeProvider.class))
                        .build());
        return fields;
    }

    private static List<Field> baseServiceClientConfigurationFields() {
        return Arrays.asList(
            Field.builder()
                 .doc("client override configuration")
                 .name("overrideConfiguration")
                 .type(ClientOverrideConfiguration.class)
                 .build(),
            Field.builder()
                 .doc("AWS region")
                 .name("region")
                 .type(Region.class)
                 .optionClass(AwsClientOption.class)
                 .optionName("AWS_REGION")
                 .build(),
            Field.builder()
                 .doc("endpoint override")
                 .name("endpointOverride")
                 .type(URI.class)
                 .optionClass(SdkInternalAdvancedClientOption.class)
                 .optionName("ENDPOINT_OVERRIDE_VALUE")
                 .build(),
            Field.builder()
                 .name("endpointProvider")
                 .type(EndpointProvider.class)
                 .doc("endpoint provider")
                 .optionClass(SdkClientOption.class)
                 .optionName("ENDPOINT_PROVIDER")
                 .build(),
            Field.builder()
                 .name("credentialsProvider")
                 .type(ParameterizedTypeName.get(ClassName.get(IdentityProvider.class),
                                                 WildcardTypeName.subtypeOf(AwsCredentialsIdentity.class)))
                 .doc("credentials provider")
                 .optionClass(AwsClientOption.class)
                 .optionName("CREDENTIALS_IDENTITY_PROVIDER")
                 .build()
        );
    }

    static class Field {
        private final String name;
        private final TypeName type;
        private final Class<? extends ClientOption> optionClass;
        private final String optionName;
        private final String doc;
        private final boolean isInherited;
        private final TypeName baseType;

        Field(Builder builder) {
            this.name = Validate.paramNotNull(builder.name, "name");
            this.type = Validate.paramNotNull(builder.type, "type");
            this.doc = Validate.paramNotNull(builder.doc, "doc");
            this.optionClass = builder.optionClass;
            this.optionName = builder.optionName;
            this.isInherited = builder.isInherited;
            this.baseType = builder.baseType;
        }

        public static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private String name;
            private TypeName type;
            private String doc;
            private Class<? extends ClientOption> optionClass;
            private String optionName;
            private boolean isInherited = true;
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

            public Field.Builder optionName(String optionName) {
                this.optionName = optionName;
                return this;
            }

            public Field.Builder isInherited(boolean isInherited) {
                this.isInherited = isInherited;
                return this;
            }

            public Field.Builder baseType(TypeName baseType) {
                this.baseType = baseType;
                return this;
            }

            public Field build() {
                return new Field(this);
            }
        }
    }
}
