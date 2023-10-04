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
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.awscore.AwsServiceClientConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeSpecUtils;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.config.internal.SdkClientConfigurationUtil;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;

public class ServiceClientConfigurationUtils {
    private static final List<Field> BASE_FIELDS = baseServiceClientConfigurationFields();
    private final AuthSchemeSpecUtils authSchemeSpecUtils;
    private final ClassName configurationClassName;
    private final ClassName configurationBuilderClassName;


    public ServiceClientConfigurationUtils(IntermediateModel model) {
        String basePackage = model.getMetadata().getFullClientPackageName();
        String serviceId = model.getMetadata().getServiceName();
        configurationClassName = ClassName.get(basePackage, serviceId + "ServiceClientConfiguration");
        configurationBuilderClassName = ClassName.get(model.getMetadata().getFullClientInternalPackageName(),
                                                      serviceId + "ServiceClientConfigurationBuilder");
        this.authSchemeSpecUtils = new AuthSchemeSpecUtils(model);
    }

    /**
     * Returns the {@link ClassName} of the service client configuration class.
     */
    public ClassName serviceClientConfigurationClassName() {
        return configurationClassName;
    }

    /**
     * Returns the {@link ClassName} of the builder for the service client configuration class.
     */
    public ClassName serviceClientConfigurationBuilderClassName() {
        return configurationBuilderClassName;
    }

    /**
     * Returns the list of fields present in the service client configuration class with its corresponding {@link ClientOption}
     * mapping to the {@link SdkClientConfiguration} class.
     */
    public List<Field> serviceClientConfigurationFields() {
        List<Field> fields = new ArrayList<>(BASE_FIELDS);
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
            overrideConfigurationField(),
            endpointOverrideField(),
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
            credentialsProviderField()
        );
    }

    private static Field endpointOverrideField() {
        Field.Builder builder = Field.builder("endpointOverride", URI.class)
                                     .doc("endpoint override")
                                     .definingClass(SdkServiceClientConfiguration.class);
        builder.constructFromConfiguration(
            CodeBlock.builder()
                     .beginControlFlow("if (Boolean.TRUE.equals(internalBuilder.option($T.$L)))",
                                       SdkClientOption.class, fieldName(SdkClientOption.ENDPOINT_OVERRIDDEN,
                                                                        SdkClientOption.class))
                     .addStatement("this.endpointOverride = internalBuilder.option($T.$L)",
                                   SdkClientOption.class, fieldName(SdkClientOption.ENDPOINT,
                                                                    SdkClientOption.class))
                     .endControlFlow()
                     .build()
        );

        builder.copyToConfiguration(
            CodeBlock.builder()
                     .beginControlFlow("if (endpointOverride != null)")
                     .addStatement("internalBuilder.option($T.$L, endpointOverride)",
                                   SdkClientOption.class, fieldName(SdkClientOption.ENDPOINT, SdkClientOption.class))
                     .addStatement("internalBuilder.option($T.$L, true)",
                                   SdkClientOption.class, fieldName(SdkClientOption.ENDPOINT_OVERRIDDEN, SdkClientOption.class))
                     .endControlFlow()
                     .build()
        );

        return builder.build();
    }

    private static Field credentialsProviderField() {
        Field.Builder builder = Field.builder("credentialsProvider",
                                              ParameterizedTypeName.get(ClassName.get(IdentityProvider.class),
                                                                        WildcardTypeName.subtypeOf(AwsCredentialsIdentity.class)))
                                     .doc("credentials provider")
                                     .definingClass(AwsServiceClientConfiguration.class);

        builder.constructFromConfiguration(
            CodeBlock.builder()
                     .addStatement("this.credentialsProvider = internalBuilder.option($T.$L)",
                                   AwsClientOption.class, fieldName(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER,
                                                                    AwsClientOption.class))
                     .build()
        );

        builder.copyToConfiguration(
            // TODO(sra-plugins)
            // This code duplicates the logic here
            // https://github.com/aws/aws-sdk-java-v2/blob/fa9dbcce47637486e3f7d4d366ab6509b535342a/core/aws-core/src/main/java/software/amazon/awssdk/awscore/client/builder/AwsDefaultClientBuilder.java#L212
            // That adds the credentialsProvider to the identityProviders class. This is for request level plugins,
            // to be able to support credentialsProvider overrides.
            CodeBlock.builder()
                     .beginControlFlow("if (credentialsProvider != null &&"
                                       + " !credentialsProvider.equals(internalBuilder.option($T.$L)))",
                                       AwsClientOption.class, fieldName(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER,
                                                                        AwsClientOption.class))
                     .addStatement("internalBuilder.option($T.$L, credentialsProvider)",
                                   AwsClientOption.class, fieldName(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER,
                                                                    AwsClientOption.class))
                     .addStatement("$T identityProviders = internalBuilder.option($T.$L)",
                                   IdentityProviders.class, SdkClientOption.class,
                                   fieldName(SdkClientOption.IDENTITY_PROVIDERS, SdkClientOption.class))
                     .beginControlFlow("if (identityProviders == null)")
                     .addStatement("identityProviders = $T.builder().putIdentityProvider(credentialsProvider).build()",
                                   IdentityProviders.class)
                     .nextControlFlow(" else ")
                     .addStatement("identityProviders = identityProviders.toBuilder()"
                                   + ".putIdentityProvider(credentialsProvider)"
                                   + ".build()")
                     .endControlFlow()
                     .addStatement("internalBuilder.option($T.$L, identityProviders)",
                                   SdkClientOption.class, fieldName(SdkClientOption.IDENTITY_PROVIDERS, SdkClientOption.class))
                     .endControlFlow()
                     .build()
        );

        return builder.build();
    }

    private static Field overrideConfigurationField() {
        Field.Builder builder = Field.builder("overrideConfiguration", ClientOverrideConfiguration.class)
                                     .doc("client override configuration")
                                     .definingClass(SdkServiceClientConfiguration.class);

        builder.constructFromConfiguration(
            CodeBlock.builder()
                     .addStatement("this.overrideConfiguration = $T.copyConfigurationToOverrides("
                                   + "$T.builder(), internalBuilder).build()", SdkClientConfigurationUtil.class,
                                   ClientOverrideConfiguration.class)
                     .build()
        );

        builder.copyToConfiguration(
            CodeBlock.builder()
                     .beginControlFlow("if (overrideConfiguration != null)")
                     .addStatement("$T.copyOverridesToConfiguration(overrideConfiguration, internalBuilder)",
                                   SdkClientConfigurationUtil.class)
                     .endControlFlow()
                     .build()
        );

        return builder.build();
    }

    static class Field {
        private final String name;
        private final TypeName type;
        private final Class<? extends SdkServiceClientConfiguration> definingClass;
        private final Class<? extends ClientOption> optionClass;
        private final String optionName;
        private final String doc;
        private final TypeName baseType;
        private final CodeBlock constructFromConfiguration;
        private final CodeBlock copyToConfiguration;

        Field(Field.Builder builder) {
            this.name = Validate.paramNotNull(builder.name, "name");
            this.type = Validate.paramNotNull(builder.type, "type");
            this.definingClass = builder.definingClass;
            this.doc = Validate.paramNotNull(builder.doc, "doc");
            this.optionClass = builder.optionClass;
            this.optionName = builder.optionName;
            this.baseType = builder.baseType;
            this.constructFromConfiguration = builder.constructFromConfiguration;
            this.copyToConfiguration = builder.copyToConfiguration;
        }

        public boolean isLocalField() {
            return definingClass == null;
        }

        public String name() {
            return name;
        }

        public TypeName type() {
            return type;
        }

        public Class<? extends SdkServiceClientConfiguration> definingClass() {
            return definingClass;
        }

        public Class<? extends ClientOption> optionClass() {
            return optionClass;
        }

        public String optionName() {
            return optionName;
        }

        public String doc() {
            return doc;
        }

        public TypeName baseType() {
            return baseType;
        }

        public CodeBlock constructFromConfiguration() {
            return constructFromConfiguration;
        }

        public CodeBlock copyToConfiguration() {
            return copyToConfiguration;
        }

        public static Field.Builder builder() {
            return new Field.Builder();
        }

        public static Field.Builder builder(String name, TypeName type) {
            return new Field.Builder()
                .name(name)
                .type(type);
        }

        public static Field.Builder builder(String name, Class<?> type) {
            return new Field.Builder()
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
            private CodeBlock constructFromConfiguration;
            private CodeBlock copyToConfiguration;


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

            public Field.Builder constructFromConfiguration(CodeBlock constructFromConfiguration) {
                this.constructFromConfiguration = constructFromConfiguration;
                return this;
            }

            public Field.Builder copyToConfiguration(CodeBlock copyToConfiguration) {
                this.copyToConfiguration = copyToConfiguration;
                return this;
            }

            public Field build() {
                if (value != null && optionClass != null) {
                    optionName = fieldName(value, optionClass);
                }
                return new Field(this);
            }
        }
    }

    /**
     * This method resolves an static reference to its name, for instance, when called with
     * <pre>
     * fieldName(AwsClientOption.AWS_REGION, AwsClientOption.class)
     * </pre>
     * it will return the string "AWS_REGION" that we can use for codegen. Using the value directly avoid typo bugs and allows the
     * compiler and the IDE to know about this relationship.
     * <p>
     * This method uses the fully qualified names in the reflection package to avoid polluting this class imports. Adapted from
     * https://stackoverflow.com/a/35416606
     */
    private static String fieldName(Object fieldObject, Class<?> parent) {
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
