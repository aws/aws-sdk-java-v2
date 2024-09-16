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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeSpecUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Validate;

public class ServiceClientConfigurationUtils {
    private final AuthSchemeSpecUtils authSchemeSpecUtils;
    private final ClassName configurationClassName;
    private final ClassName configurationBuilderClassName;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;
    private final List<Field> fields;

    public ServiceClientConfigurationUtils(IntermediateModel model) {
        String basePackage = model.getMetadata().getFullClientPackageName();
        String serviceId = model.getMetadata().getServiceName();
        configurationClassName = ClassName.get(basePackage, serviceId + "ServiceClientConfiguration");
        configurationBuilderClassName = ClassName.get(model.getMetadata().getFullClientInternalPackageName(),
                                                      serviceId + "ServiceClientConfigurationBuilder");
        authSchemeSpecUtils = new AuthSchemeSpecUtils(model);
        endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
        fields = fields(model);
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
        return Collections.unmodifiableList(fields);
    }

    private List<Field> fields(IntermediateModel model) {
        List<Field> fields = new ArrayList<>();

        fields.addAll(Arrays.asList(
            overrideConfigurationField(),
            endpointOverrideField(),
            endpointProviderField(),
            regionField(),
            credentialsProviderField(),
            authSchemesField(),
            authSchemeProviderField()
        ));
        fields.addAll(addCustomClientParams(model));
        return fields;
    }

    private List<Field> addCustomClientParams(IntermediateModel model) {
        List<Field> customClientParamFields = new ArrayList<>();

        if (model.getCustomizationConfig() != null && model.getCustomizationConfig().getCustomClientContextParams() != null) {
            model.getCustomizationConfig().getCustomClientContextParams().forEach((n, m) -> {

                String paramName = endpointRulesSpecUtils.paramMethodName(n);
                String keyName = model.getNamingStrategy().getEnumValueName(n);
                TypeName type = endpointRulesSpecUtils.toJavaType(m.getType());

                customClientParamFields.add(fieldBuilder(paramName, type)
                      .doc(m.getDocumentation())
                      .isInherited(false)
                      .localSetter(basicLocalSetterCode(paramName))
                      .localGetter(basicLocalGetterCode(paramName))
                      .configSetter(customClientConfigParamSetter(paramName, keyName))
                      .configGetter(customClientConfigParamGetter(keyName))
                      .build());
            });
        }

        return customClientParamFields;
    }

    private Field overrideConfigurationField() {
        return fieldBuilder("overrideConfiguration", ClientOverrideConfiguration.class)
                    .doc("client override configuration")
                    .localSetter(basicLocalSetterCode("overrideConfiguration"))
                    .localGetter(basicLocalGetterCode("overrideConfiguration"))
                    .configSetter(overrideConfigurationConfigSetter())
                    .configGetter(overrideConfigurationConfigGetter())
                    .build();
    }

    private CodeBlock overrideConfigurationConfigSetter() {
        return CodeBlock.builder()
                        .addStatement("config.putAll(overrideConfiguration)",
                                      SdkClientConfiguration.class)
                        .addStatement("return this")
                        .build();
    }

    private CodeBlock overrideConfigurationConfigGetter() {
        return CodeBlock.of("return config.asOverrideConfigurationBuilder().build();");
    }

    private Field endpointOverrideField() {
        return fieldBuilder("endpointOverride", URI.class)
                    .doc("endpoint override")
                    .localSetter(basicLocalSetterCode("endpointOverride"))
                    .localGetter(basicLocalGetterCode("endpointOverride"))
                    .configSetter(endpointOverrideConfigSetter())
                    .configGetter(endpointOverrideConfigGetter())
                    .build();
    }

    private CodeBlock endpointOverrideConfigSetter() {
        return CodeBlock.builder()
                        .beginControlFlow("if (endpointOverride != null)")
                        .addStatement("config.option($T.CLIENT_ENDPOINT_PROVIDER, $T.forEndpointOverride(endpointOverride))",
                                      SdkClientOption.class, ClientEndpointProvider.class)
                        .nextControlFlow("else")
                        .addStatement("config.option($T.CLIENT_ENDPOINT_PROVIDER, null)", SdkClientOption.class)
                        .endControlFlow()
                        .addStatement("return this")
                        .build();
    }

    private CodeBlock endpointOverrideConfigGetter() {
        return CodeBlock.builder()
                        .addStatement("$T clientEndpoint = config.option($T.CLIENT_ENDPOINT_PROVIDER)",
                                      ClientEndpointProvider.class, SdkClientOption.class)
                        .beginControlFlow("if (clientEndpoint != null && clientEndpoint.isEndpointOverridden())")
                        .addStatement("return clientEndpoint.clientEndpoint()")
                        .endControlFlow()
                        .addStatement("return null")
                        .build();
    }

    private Field endpointProviderField() {
        return fieldBuilder("endpointProvider", EndpointProvider.class)
                    .doc("endpoint provider")
                    .localSetter(basicLocalSetterCode("endpointProvider"))
                    .localGetter(basicLocalGetterCode("endpointProvider"))
                    .configSetter(basicConfigSetterCode(SdkClientOption.ENDPOINT_PROVIDER, "endpointProvider"))
                    .configGetter(basicConfigGetterCode(SdkClientOption.ENDPOINT_PROVIDER))
                    .build();
    }

    private Field regionField() {
        return fieldBuilder("region", Region.class)
                    .doc("AWS region")
                    .localSetter(basicLocalSetterCode("region"))
                    .localGetter(basicLocalGetterCode("region"))
                    .configSetter(basicConfigSetterCode(AwsClientOption.AWS_REGION, "region"))
                    .configGetter(basicConfigGetterCode(AwsClientOption.AWS_REGION))
                    .build();
    }

    private Field credentialsProviderField() {
        TypeName awsIdentityProviderType =
            ParameterizedTypeName.get(ClassName.get(IdentityProvider.class),
                                      WildcardTypeName.subtypeOf(AwsCredentialsIdentity.class));

        return fieldBuilder("credentialsProvider", awsIdentityProviderType)
                    .doc("credentials provider")
                    .localSetter(basicLocalSetterCode("credentialsProvider"))
                    .localGetter(basicLocalGetterCode("credentialsProvider"))
                    .configSetter(basicConfigSetterCode(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER, "credentialsProvider"))
                    .configGetter(basicConfigGetterCode(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER))
                    .build();
    }


    private Field authSchemesField() {
        TypeName authSchemeGenericType = ParameterizedTypeName.get(ClassName.get(AuthScheme.class),
                                                                   WildcardTypeName.subtypeOf(Object.class));
        TypeName authSchemesType = ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class),
                                                             authSchemeGenericType);

        return fieldBuilder("authSchemes", authSchemesType)
                    .doc("auth schemes")
                    .setterSpec(authSchemesSetterSpec())
                    .localSetter(authSchemesLocalSetter())
                    .localGetter(authSchemesLocalGetter())
                    .configSetter(authSchemesConfigSetter())
                    .configGetter(authSchemeConfigGetter())
                    .build();
    }

    private MethodSpec authSchemesSetterSpec() {
        TypeName authScheme = ParameterizedTypeName.get(ClassName.get(AuthScheme.class),
                                                        WildcardTypeName.subtypeOf(Object.class));
        return MethodSpec.methodBuilder("putAuthScheme")
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(authScheme, "authScheme")
                         .returns(configurationClassName.nestedClass("Builder"))
                         .build();
    }

    private CodeBlock authSchemesLocalSetter() {
        return CodeBlock.builder()
                        .beginControlFlow("if (this.authSchemes == null)")
                        .addStatement("this.authSchemes = new HashMap<>()")
                        .endControlFlow()
                        .addStatement("this.authSchemes.put(authScheme.schemeId(), authScheme)")
                        .addStatement("return this")
                        .build();
    }

    private CodeBlock authSchemesLocalGetter() {
        return CodeBlock.builder()
                        .addStatement("return $1T.unmodifiableMap(authSchemes == null ? $1T.emptyMap() : authSchemes)",
                                      Collections.class)
                        .build();
    }

    private CodeBlock authSchemesConfigSetter() {
        return CodeBlock.builder()
                        .addStatement("config.computeOptionIfAbsent($T.AUTH_SCHEMES, $T::new)"
                                      + ".put(authScheme.schemeId(), authScheme)",
                                      SdkClientOption.class, HashMap.class)
                        .addStatement("return this")
                        .build();
    }

    private CodeBlock authSchemeConfigGetter() {
        return CodeBlock.builder()
                        .addStatement("$T<$T, $T<?>> authSchemes = config.option($T.AUTH_SCHEMES)",
                                      Map.class, String.class, AuthScheme.class, SdkClientOption.class)
                        .addStatement("return $1T.unmodifiableMap(authSchemes == null ? $1T.emptyMap() : authSchemes)",
                                      Collections.class)
                        .build();
    }

    private Field authSchemeProviderField() {
        return fieldBuilder("authSchemeProvider", authSchemeSpecUtils.providerInterfaceName())
                    .doc("auth scheme provider")
                    .isInherited(false)
                    .localSetter(basicLocalSetterCode("authSchemeProvider"))
                    .localGetter(basicLocalGetterCode("authSchemeProvider"))
                    .configSetter(basicConfigSetterCode(SdkClientOption.AUTH_SCHEME_PROVIDER, "authSchemeProvider"))
                    .configGetter(authSchemeProviderConfigGetter())
                    .build();
    }

    private CodeBlock authSchemeProviderConfigGetter() {
        return CodeBlock.builder()
                        .addStatement("$T result = config.option($T.AUTH_SCHEME_PROVIDER)",
                                      AuthSchemeProvider.class, SdkClientOption.class)
                        .beginControlFlow("if (result == null)")
                        .addStatement("return null")
                        .endControlFlow()
                        .addStatement("return $1T.isInstanceOf($2T.class, result, \"Expected an instance of \" + $2T.class"
                                      + ".getSimpleName())",
                                      Validate.class, authSchemeSpecUtils.providerInterfaceName())
                        .build();
    }

    private CodeBlock customClientConfigParamSetter(String parameterName, String keyName) {
        return CodeBlock.builder()
                        .addStatement("config.option($1T.CLIENT_CONTEXT_PARAMS, "
                                      + "config.computeOptionIfAbsent($1T.CLIENT_CONTEXT_PARAMS, $2T::empty)"
                                      + ".toBuilder().put($3T.$4N, $5N).build())",
                                      SdkClientOption.class,
                                      AttributeMap.class,
                                      endpointRulesSpecUtils.clientContextParamsName(),
                                      keyName, parameterName)
                        .addStatement("return this")
                        .build();
    }

    private CodeBlock customClientConfigParamGetter(String keyName) {
        return CodeBlock.builder()
                        .addStatement("return config.computeOptionIfAbsent($T.CLIENT_CONTEXT_PARAMS, $T::empty)\n"
                                      + ".get($T.$N)", SdkClientOption.class, AttributeMap.class,
                                      endpointRulesSpecUtils.clientContextParamsName(), keyName)
                        .build();
    }

    private CodeBlock basicLocalSetterCode(String fieldName) {
        return CodeBlock.builder()
                        .addStatement("this.$1N = $1N", fieldName)
                        .addStatement("return this")
                        .build();
    }

    private CodeBlock basicLocalGetterCode(String fieldName) {
        return CodeBlock.of("return $N;", fieldName);
    }

    private CodeBlock basicConfigSetterCode(ClientOption<?> option, String parameterName) {
        return CodeBlock.builder()
                        .addStatement("config.option($T.$N, $N)", option.getClass(), fieldName(option), parameterName)
                        .addStatement("return this")
                        .build();
    }

    private CodeBlock basicConfigGetterCode(ClientOption<?> option) {
        return CodeBlock.of("return config.option($T.$N);", option.getClass(), fieldName(option));
    }

    public class Field {
        private final String name;
        private final TypeName type;
        private final String doc;
        private final boolean isInherited;
        private final MethodSpec setterSpec;
        private final MethodSpec getterSpec;
        private final MethodSpec localSetter;
        private final MethodSpec localGetter;
        private final MethodSpec configSetter;
        private final MethodSpec configGetter;

        Field(FieldBuilder builder) {
            this.name = Validate.paramNotNull(builder.name, "name");
            this.type = Validate.paramNotNull(builder.type, "type");
            this.doc = Validate.paramNotNull(builder.doc, "doc");
            this.isInherited = Validate.paramNotNull(builder.isInherited, "isInherited");
            Validate.paramNotNull(builder.localSetter, "localSetter");
            Validate.paramNotNull(builder.localGetter, "localGetter");
            Validate.paramNotNull(builder.configSetter, "configSetter");
            Validate.paramNotNull(builder.configGetter, "configGetter");

            this.setterSpec = setterSpec(builder.setterSpec);
            this.getterSpec = getterSpec(builder.getterSpec);
            this.localSetter = setterSpec.toBuilder().addCode(builder.localSetter).build();
            this.localGetter = getterSpec.toBuilder().addCode(builder.localGetter).build();
            this.configSetter = setterSpec.toBuilder().addCode(builder.configSetter).build();
            this.configGetter = getterSpec.toBuilder().addCode(builder.configGetter).build();
        }

        private MethodSpec setterSpec(MethodSpec setterSpec) {
            if (setterSpec != null) {
                return setterSpec;
            }
            return MethodSpec.methodBuilder(name)
                             .addJavadoc("Sets the value for " + doc)
                             .addModifiers(Modifier.PUBLIC)
                             .returns(configurationClassName.nestedClass("Builder"))
                             .addParameter(type, name)
                             .build();
        }

        private MethodSpec getterSpec(MethodSpec getterSpec) {
            if (getterSpec != null) {
                return getterSpec;
            }
            return MethodSpec.methodBuilder(name)
                             .addJavadoc("Gets the value for " + doc)
                             .addModifiers(Modifier.PUBLIC)
                             .returns(type)
                             .build();
        }

        public String name() {
            return name;
        }

        public TypeName type() {
            return type;
        }

        public String doc() {
            return doc;
        }

        public boolean isInherited() {
            return isInherited;
        }

        public MethodSpec setterSpec() {
            return setterSpec;
        }

        public MethodSpec getterSpec() {
            return getterSpec;
        }

        public MethodSpec localSetter() {
            return localSetter;
        }

        public MethodSpec localGetter() {
            return localGetter;
        }

        public MethodSpec configSetter() {
            return configSetter;
        }

        public MethodSpec configGetter() {
            return configGetter;
        }
    }

    public FieldBuilder fieldBuilder(String name, TypeName type) {
        return new FieldBuilder().name(name).type(type);
    }

    public FieldBuilder fieldBuilder(String name, Class<?> type) {
        return new FieldBuilder().name(name).type(type);
    }

    private class FieldBuilder {
        private String name;
        private TypeName type;
        private String doc;
        private Boolean isInherited = true;
        private MethodSpec setterSpec;
        private MethodSpec getterSpec;
        private CodeBlock localSetter;
        private CodeBlock localGetter;
        private CodeBlock configSetter;
        private CodeBlock configGetter;

        public FieldBuilder name(String name) {
            this.name = name;
            return this;
        }

        public FieldBuilder type(Class<?> type) {
            this.type = ClassName.get(type);
            return this;
        }

        public FieldBuilder type(TypeName type) {
            this.type = type;
            return this;
        }

        public FieldBuilder doc(String doc) {
            this.doc = doc;
            return this;
        }

        public FieldBuilder isInherited(Boolean inherited) {
            isInherited = inherited;
            return this;
        }

        public FieldBuilder setterSpec(MethodSpec setterSpec) {
            this.setterSpec = setterSpec;
            return this;
        }

        public FieldBuilder getterSpec(MethodSpec getterSpec) {
            this.getterSpec = getterSpec;
            return this;
        }

        public FieldBuilder localSetter(CodeBlock localSetter) {
            this.localSetter = localSetter;
            return this;
        }

        public FieldBuilder localGetter(CodeBlock localGetter) {
            this.localGetter = localGetter;
            return this;
        }

        public FieldBuilder configSetter(CodeBlock configSetter) {
            this.configSetter = configSetter;
            return this;
        }

        public FieldBuilder configGetter(CodeBlock configGetter) {
            this.configGetter = configGetter;
            return this;
        }

        public Field build() {
            return new Field(this);
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
    private static String fieldName(Object fieldObject) {
        java.lang.reflect.Field[] allFields = fieldObject.getClass().getFields();
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
                                                                 fieldObject.getClass().getName()));
    }
}
