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

package software.amazon.awssdk.codegen.poet.auth.scheme;

import static software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeCodegenMetadata.Builder;
import static software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeCodegenMetadata.builder;

import com.squareup.javapoet.CodeBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeCodegenMetadata.SignerPropertyValueProvider;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.scheme.BearerAuthScheme;
import software.amazon.awssdk.http.auth.scheme.NoAuthAuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;

/**
 * Extension and utility methods for the {@link AuthSchemeCodegenMetadata} class.
 */
public final class AuthSchemeCodegenMetadataExt {

    static final AuthSchemeCodegenMetadata SIGV4 =
        builder()
            .schemeId(AwsV4AuthScheme.SCHEME_ID)
            .authSchemeClass(AwsV4AuthScheme.class)
            .addProperty(SignerPropertyValueProvider.builder()
                                                    .containingClass(AwsV4HttpSigner.class)
                                                    .fieldName(
                                                        "SERVICE_SIGNING_NAME")
                                                    .valueEmitter((spec, utils) -> spec.add("$S", utils.signingName()))
                                                    .build())
            .addProperty(SignerPropertyValueProvider.builder()
                                                    .containingClass(AwsV4HttpSigner.class)
                                                    .fieldName(
                                                        "REGION_NAME")
                                                    .valueEmitter((spec, utils) -> spec.add("$L", "params.region().id()"))
                                                    .build())
            .build();

    static final AuthSchemeCodegenMetadata BEARER = builder()
        .schemeId(BearerAuthScheme.SCHEME_ID)
        .authSchemeClass(BearerAuthScheme.class)
        .build();

    static final AuthSchemeCodegenMetadata SIGV4A =
        builder()
            .schemeId(AwsV4aAuthScheme.SCHEME_ID)
            .authSchemeClass(AwsV4aAuthScheme.class)
            .addProperty(SignerPropertyValueProvider.builder()
                                                    .containingClass(AwsV4aHttpSigner.class)
                                                    .fieldName(
                                                        "SERVICE_SIGNING_NAME")
                                                    .valueEmitter((spec, utils) -> spec.add("$S", utils.signingName()))
                                                    .build())
            .addProperty(SignerPropertyValueProvider.builder()
                                                    .containingClass(AwsV4aHttpSigner.class)
                                                    .fieldName(
                                                        "REGION_SET")
                                                    .valueEmitter((spec, utils) -> spec.add("$L", "params.regionSet()"))
                                                    .build())
            .build();

    static final AuthSchemeCodegenMetadata NO_AUTH = builder()
        .schemeId(NoAuthAuthScheme.SCHEME_ID)
        .authSchemeClass(NoAuthAuthScheme.class)
        .build();


    private AuthSchemeCodegenMetadataExt() {
    }

    /**
     * Creates a new auth scheme codegen metadata instance using the defaults for the given {@link AuthType} defaults.
     */
    public static AuthSchemeCodegenMetadata fromAuthType(AuthOption authOption) {
        switch (authOption.authType()) {
            case BEARER:
                return BEARER;
            case NONE:
                return NO_AUTH;
            case V4A:
                return getSigv4aAuthSchemeBuilder(authOption).build();
            default:
                return resolveAuthSchemeForType(authOption);
        }
    }

    private static AuthSchemeCodegenMetadata resolveAuthSchemeForType(AuthOption authOption) {
        String authTypeName = authOption.authType().value();
        SigV4SignerDefaults defaults = AuthTypeToSigV4Default.authTypeToDefaults().get(authTypeName);

        if (defaults == null) {
            throw new IllegalArgumentException("Unknown auth option: " + authOption + " with type " + authTypeName);
        }
        if (authOption.isUnsignedPayload()) {
            defaults = defaults.toBuilder()
                               .payloadSigningEnabled(false)
                               .build();
        }
        return fromConstants(defaults);
    }

    /**
     * Transforms a {@link SigV4SignerDefaults} instance to an {@link AuthSchemeCodegenMetadata} instance.
     */
    public static AuthSchemeCodegenMetadata fromConstants(SigV4SignerDefaults constants) {
        Builder builder = SIGV4.toBuilder();
        for (SignerPropertyValueProvider property : propertiesFromConstants(constants)) {
            builder.addProperty(property);
        }
        return builder.build();
    }

    /**
     * Renders the AuthSchemeCodegenMetadata as to create a new {@link AuthSchemeOption} using the configured values.
     */
    public static CodeBlock codegenNewAuthOption(
        AuthSchemeCodegenMetadata metadata,
        AuthSchemeSpecUtils authSchemeSpecUtils
    ) {
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.add("$T.builder().schemeId($S)",
                    AuthSchemeOption.class, metadata.schemeId());
        builder.add(codegenSignerProperties(authSchemeSpecUtils, metadata.properties()));
        return builder.build();
    }

    /**
     * Renders a chain of calls to {@link AuthSchemeOption.Builder#putSignerProperty(SignerProperty, Object)} for each of the
     * given properties.
     */
    public static CodeBlock codegenSignerProperties(
        AuthSchemeSpecUtils authSchemeSpecUtils,
        List<SignerPropertyValueProvider> properties
    ) {
        CodeBlock.Builder builder = CodeBlock.builder();
        for (SignerPropertyValueProvider property : properties) {
            builder.add("\n.putSignerProperty($T.$N, ", property.containingClass(), property.fieldName());
            property.emitValue(builder, authSchemeSpecUtils);
            builder.add(")");
        }
        return builder.build();
    }

    /**
     * Renders a chain of calls to {@link AuthSchemeOption.Builder#putSignerPropertyIfAbsent(SignerProperty, Object)} for each of
     * the given properties.
     */
    public static CodeBlock codegenSignerPropertiesIfAbsent(
        AuthSchemeSpecUtils authSchemeSpecUtils,
        List<SignerPropertyValueProvider> properties
    ) {
        CodeBlock.Builder builder = CodeBlock.builder();
        for (SignerPropertyValueProvider property : properties) {
            builder.add("\n.putSignerPropertyIfAbsent($T.$N, ", property.containingClass(), property.fieldName());
            property.emitValue(builder, authSchemeSpecUtils);
            builder.add(")");
        }
        return builder.build();
    }

    private static List<SignerPropertyValueProvider> propertiesFromConstants(SigV4SignerDefaults constants) {
        List<SignerPropertyValueProvider> properties = new ArrayList<>();
        if (constants.payloadSigningEnabled() != null) {
            properties.add(from("PAYLOAD_SIGNING_ENABLED", constants::payloadSigningEnabled, AwsV4HttpSigner.class));
        }
        if (constants.doubleUrlEncode() != null) {
            properties.add(from("DOUBLE_URL_ENCODE", constants::doubleUrlEncode, AwsV4HttpSigner.class));
        }
        if (constants.normalizePath() != null) {
            properties.add(from("NORMALIZE_PATH", constants::normalizePath, AwsV4HttpSigner.class));
        }
        if (constants.chunkEncodingEnabled() != null) {
            properties.add(from("CHUNK_ENCODING_ENABLED", constants::chunkEncodingEnabled, AwsV4HttpSigner.class));
        }
        return properties;
    }

    private static SignerPropertyValueProvider from(String name,
                                                    Supplier<Object> valueSupplier,
                                                    Class<? extends HttpSigner> containingClass) {
        return SignerPropertyValueProvider.builder()
                                          .containingClass(containingClass)
                                          .fieldName(name)
                                          .constantValueSupplier(valueSupplier)
                                          .build();
    }

    private static Builder getSigv4aAuthSchemeBuilder(AuthOption authOption) {
        Builder sigv4aBuilder = builder()
            .schemeId(AwsV4aAuthScheme.SCHEME_ID)
            .authSchemeClass(AwsV4aAuthScheme.class);

        addCommonSigv4aProperties(sigv4aBuilder);

        if (authOption.isUnsignedPayload()) {
            sigv4aBuilder.addProperty(from("PAYLOAD_SIGNING_ENABLED", () -> false, AwsV4aHttpSigner.class));
        }
        return sigv4aBuilder;
    }

    private static void addCommonSigv4aProperties(Builder builder) {
        builder.addProperty(SignerPropertyValueProvider.builder()
                                                       .containingClass(AwsV4aHttpSigner.class)
                                                       .fieldName("SERVICE_SIGNING_NAME")
                                                       .valueEmitter((spec, utils) -> spec.add("$S", utils.signingName()))
                                                       .build())
               .addProperty(SignerPropertyValueProvider.builder()
                                                       .containingClass(AwsV4aHttpSigner.class)
                                                       .fieldName("REGION_SET")
                                                       .valueEmitter((spec, utils) -> spec.add("$L", "params.regionSet()"))
                                                       .build());
    }

}
